package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.IdFieldCacheKeyResolver
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
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
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
  }

  @After
  @Throws(IOException::class)
  fun shutdown() {
    server.shutdown()
  }

  internal class TrackingCallback : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
    val responseList: MutableList<ApolloResponse<EpisodeHeroNameQuery.Data>> = ArrayList()
    val exceptions: MutableList<Exception> = ArrayList()

    @Volatile
    var completed = false

    override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {
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
