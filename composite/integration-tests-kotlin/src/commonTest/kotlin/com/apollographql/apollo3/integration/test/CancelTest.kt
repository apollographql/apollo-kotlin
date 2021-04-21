package com.apollographql.apollo3.integration.test

import EpisodeHeroNameQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.integration.MockServer
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.fixtureResponse
import com.apollographql.apollo3.testing.runBlocking
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import type.Episode
import kotlin.test.BeforeTest
import kotlin.test.Test

class CancelTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url())
  }

  @Test
  @Throws(Exception::class)
  fun cancelFlow() {
    mockServer.enqueue(fixtureResponse("resources/EpisodeHeroNameResponse.json"))

    runWithMainLoop {
      val job = launch {
        delay(100)
        apolloClient.query(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))).collect {
          error("The Flow should have been canceled before reaching that state")
        }
      }
      job.cancel()
      job.join()
    }
  }

//  @Test
//  @Throws(Exception::class)
//  fun cancelCallAfterEnqueueNoCallback() {
//    val okHttpClient = OkHttpClient.Builder()
//        .dispatcher(Dispatcher(immediateExecutorService()))
//        .build()
//    apolloClient = ApolloClient.builder()
//        .serverUrl(server.url("/"))
//        .okHttpClient(okHttpClient)
//        .httpCache(ApolloHttpCache(cacheStore, null))
//        .build()
//    server.enqueue(mockResponse("EpisodeHeroNameResponse.json").setHeadersDelay(500, TimeUnit.MILLISECONDS))
//    val call: ApolloCall<EpisodeHeroNameQuery.Data> = apolloClient.query(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)))
//
//    val callback = TestableCallback<EpisodeHeroNameQuery.Data>()
//
//    call.enqueue(callback)
//    call.cancel()
//
//    try {
//      callback.waitForCompletion(1, TimeUnit.SECONDS)
//      fail("TimeoutException expected")
//    } catch (e: TimeoutException) {
//
//    }
//    Truth.assertThat(callback.responses.size).isEqualTo(0)
//    Truth.assertThat(callback.errors.size).isEqualTo(0)
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun cancelPrefetchBeforeEnqueueCanceledException() {
//    server.enqueue(mockResponse("EpisodeHeroNameResponse.json"))
//    val call = apolloClient.prefetch(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)))
//
//    val callback = TestablePrefetchCallback()
//
//    call.cancel()
//    call.enqueue(callback)
//
//    callback.waitForCompletion(1, TimeUnit.SECONDS)
//    Truth.assertThat(callback.errors.size).isEqualTo(1)
//    Truth.assertThat(callback.errors[0]).isInstanceOf(ApolloCanceledException::class.java)
//  }
//
//  @Test
//  @Throws(Exception::class)
//  fun cancelPrefetchAfterEnqueueNoCallback() {
//    server.enqueue(mockResponse("EpisodeHeroNameResponse.json").setHeadersDelay(500, TimeUnit.MILLISECONDS))
//    val call = apolloClient.prefetch(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)))
//
//    val callback = TestablePrefetchCallback()
//
//    call.enqueue(callback)
//    call.cancel()
//
//    try {
//      callback.waitForCompletion(1, TimeUnit.SECONDS)
//      fail("TimeoutException expected")
//    } catch (e: TimeoutException) {
//
//    }
//    Truth.assertThat(callback.errors.size).isEqualTo(0)
//  }


}
