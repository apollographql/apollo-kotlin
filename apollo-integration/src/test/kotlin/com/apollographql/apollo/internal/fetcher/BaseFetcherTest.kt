package com.apollographql.apollo.internal.fetcher

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.IdFieldCacheKeyResolver
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

open class BaseFetcherTest {
  lateinit var apolloClient: ApolloClient
  lateinit var server: MockWebServer

  @Before
  fun setUp() {
    server = MockWebServer()
    val okHttpClient = OkHttpClient.Builder()
        .writeTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server!!.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
  }

  @After
  @Throws(IOException::class)
  fun shutdown() {
    server!!.shutdown()
  }

  internal class TrackingCallback : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
    val responseList: MutableList<Response<EpisodeHeroNameQuery.Data>> = ArrayList()
    val exceptions: MutableList<Exception> = ArrayList()

    @Volatile
    var completed = false

    override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
      check(!completed) { "onCompleted already called Do not reuse tracking callback." }
      responseList.add(response)
    }

    override fun onFailure(e: ApolloException) {
      check(!completed) { "onCompleted already called Do not reuse tracking callback." }
      exceptions.add(e)
    }

    override fun onStatusEvent(event: ApolloCall.StatusEvent) {
      if (event == ApolloCall.StatusEvent.COMPLETED) {
        check(!completed) { "onCompleted already called Do not reuse tracking callback." }
        completed = true
      }
    }
  }

  @Throws(IOException::class)
  fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }
}