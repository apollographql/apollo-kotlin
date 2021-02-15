package com.apollographql.apollo

import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.rx2.Rx2Apollo
import io.reactivex.Observable
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.*

class AsyncNormalizedCacheTestCase {
  private lateinit var apolloClient: ApolloClient

  val server = MockWebServer()
  @Before
  fun setUp() {
    val okHttpClient = OkHttpClient.Builder()
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .build()
  }

  @Throws(IOException::class, ApolloException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }

  @Test
  @Throws(IOException::class, InterruptedException::class, ApolloException::class)
  fun testAsync() {
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    for (i in 0..499) {
      server.enqueue(mockResponse("HeroNameResponse.json"))
    }
    val calls: MutableList<Observable<Response<EpisodeHeroNameQuery.Data>>> = ArrayList()
    for (i in 0..999) {
      val queryCall = apolloClient
          .query(query)
          .responseFetcher(if (i % 2 == 0) ApolloResponseFetchers.NETWORK_FIRST else ApolloResponseFetchers.CACHE_ONLY)
      calls.add(Rx2Apollo.from(queryCall))
    }
    val observer = TestObserver<Response<EpisodeHeroNameQuery.Data>>()
    Observable.merge(calls).subscribe(observer)
    observer.awaitTerminalEvent()
    observer.assertNoErrors()
    observer.assertValueCount(1000)
    observer.assertNever(Predicate<Response<EpisodeHeroNameQuery.Data>> { dataResponse -> dataResponse.hasErrors() })
  }
}
