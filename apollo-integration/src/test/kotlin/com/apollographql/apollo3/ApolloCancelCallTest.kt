package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.cache.http.ApolloHttpCache
import com.apollographql.apollo3.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo3.exception.ApolloCanceledException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.fail

class ApolloCancelCallTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var cacheStore: MockHttpCacheStore

  @get:Rule
  val server = MockWebServer()

  @Before
  fun setup() {
    cacheStore = MockHttpCacheStore()
    cacheStore.delegate = DiskLruHttpCacheStore(InMemoryFileSystem(), File("/cache/"), Int.MAX_VALUE.toLong())

    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .httpCache(ApolloHttpCache(cacheStore, null))
        .build()
  }

  class TestableCallback<D: Operation.Data> : ApolloCall.Callback<D>() {
    val lock = Object()
    var completed = false
    val responses = mutableListOf<Response<D>>()
    val errors = mutableListOf<ApolloException>()

    override fun onResponse(response: Response<D>) {
      synchronized(lock) {
        responses.add(response)
      }
    }

    override fun onFailure(e: ApolloException) {
      synchronized(lock) {
        errors.add(e)

        // Runtime doesn't send the COMPLETED status event
        completed = true
        lock.notifyAll()
      }
    }

    override fun onStatusEvent(event: ApolloCall.StatusEvent) {
      super.onStatusEvent(event)
      if (event == ApolloCall.StatusEvent.COMPLETED) {
        synchronized(lock) {
          completed = true
          lock.notifyAll()
        }
      }
    }

    fun waitForCompletion(timeoutDuration: Long, unit: TimeUnit) {
      val start = System.currentTimeMillis()
      while (true) {
        val timeoutMillis = TimeUnit.MILLISECONDS.convert(timeoutDuration, unit) - (System.currentTimeMillis() - start)
        if (timeoutMillis <= 0) {
          throw TimeoutException("Timeout reached")
        }
        if (completed) {
          break
        }
        synchronized(lock) {
          try {
            lock.wait(timeoutMillis)
          } catch (e: InterruptedException) {

          }
        }
      }
    }
  }

  class TestablePrefetchCallback : ApolloPrefetch.Callback() {
    val lock = Object()
    var completed = false
    val errors = mutableListOf<ApolloException>()


    override fun onFailure(e: ApolloException) {
      synchronized(lock) {
        errors.add(e)

        // Runtime doesn't send the COMPLETED status event
        completed = true
        lock.notifyAll()
      }
    }

    override fun onSuccess() {
      synchronized(lock) {
        completed = true
        lock.notifyAll()
      }
    }

    fun waitForCompletion(timeoutDuration: Long, unit: TimeUnit) {
      val start = System.currentTimeMillis()
      while (true) {
        val timeoutMillis = TimeUnit.MILLISECONDS.convert(timeoutDuration, unit) - (System.currentTimeMillis() - start)
        if (timeoutMillis <= 0) {
          throw TimeoutException("Timeout reached")
        }
        if (completed) {
          break
        }
        synchronized(lock) {
          try {
            lock.wait(timeoutMillis)
          } catch (e: InterruptedException) {

          }
        }
      }
    }
  }

  @Test
  @Throws(Exception::class)
  fun cancelCallBeforeEnqueueCanceledException() {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json"))
    val call: ApolloCall<EpisodeHeroNameQuery.Data> = apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))

    val callback = TestableCallback<EpisodeHeroNameQuery.Data>()

    call.cancel()
    call.enqueue(callback)

    callback.waitForCompletion(1, TimeUnit.SECONDS)
    Truth.assertThat(callback.responses.size).isEqualTo(0)
    Truth.assertThat(callback.errors.size).isEqualTo(1)
    Truth.assertThat(callback.errors[0]).isInstanceOf(ApolloCanceledException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun cancelCallAfterEnqueueNoCallback() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .httpCache(ApolloHttpCache(cacheStore, null))
        .build()
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json").setHeadersDelay(500, TimeUnit.MILLISECONDS))
    val call: ApolloCall<EpisodeHeroNameQuery.Data> = apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))

    val callback = TestableCallback<EpisodeHeroNameQuery.Data>()

    call.enqueue(callback)
    call.cancel()

    try {
      callback.waitForCompletion(1, TimeUnit.SECONDS)
      fail("TimeoutException expected")
    } catch (e: TimeoutException) {

    }
    Truth.assertThat(callback.responses.size).isEqualTo(0)
    Truth.assertThat(callback.errors.size).isEqualTo(0)
  }

  @Test
  @Throws(Exception::class)
  fun cancelPrefetchBeforeEnqueueCanceledException() {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json"))
    val call = apolloClient.prefetch(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))

    val callback = TestablePrefetchCallback()

    call.cancel()
    call.enqueue(callback)

    callback.waitForCompletion(1, TimeUnit.SECONDS)
    Truth.assertThat(callback.errors.size).isEqualTo(1)
    Truth.assertThat(callback.errors[0]).isInstanceOf(ApolloCanceledException::class.java)
  }

  @Test
  @Throws(Exception::class)
  fun cancelPrefetchAfterEnqueueNoCallback() {
    server.enqueue(mockResponse("EpisodeHeroNameResponse.json").setHeadersDelay(500, TimeUnit.MILLISECONDS))
    val call = apolloClient.prefetch(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))

    val callback = TestablePrefetchCallback()

    call.enqueue(callback)
    call.cancel()

    try {
      callback.waitForCompletion(1, TimeUnit.SECONDS)
      fail("TimeoutException expected")
    } catch (e: TimeoutException) {

    }
    Truth.assertThat(callback.errors.size).isEqualTo(0)
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }
}
