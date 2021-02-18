package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.IdFieldCacheKeyResolver
import com.apollographql.apollo3.Utils.assertResponse
import com.apollographql.apollo3.Utils.enqueueAndAssertResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.mockResponse
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.integration.normalizer.CreateReviewMutation
import com.apollographql.apollo3.integration.normalizer.ReviewsByEpisodeQuery
import com.apollographql.apollo3.integration.normalizer.type.ColorInput
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.normalizer.type.ReviewInput
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

class QueryRefetchTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var server: MockWebServer
  @Before
  @Throws(IOException::class)
  fun setUp() {
    server = MockWebServer()
    server.start()
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .dispatcher(immediateExecutor())
        .okHttpClient(okHttpClient)
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .build()
  }

  @After
  fun tearDown() {
    try {
      server.shutdown()
    } catch (ignored: IOException) {
    }
  }

  @Test
  @Throws(Exception::class)
  fun refetchNoPreCachedQuery() {
    val mutation = CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput(stars = 5, commentary = Input.present("Awesome"), favoriteColor = ColorInput())
    )
    server.enqueue(mockResponse("CreateReviewResponse.json"))
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"))
    val call = apolloClient.mutate(mutation).refetchQueries(ReviewsByEpisodeQuery(Episode.EMPIRE)) as RealApolloCall<*>
    Rx2Apollo
        .from(call)
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(2)
    assertResponse(
        apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.reviews).hasSize(3)
      assertThat(data.reviews?.get(2)?.stars).isEqualTo(5)
      assertThat(data.reviews?.get(2)?.commentary).isEqualTo("Amazing")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun refetchPreCachedQuery() {
    enqueueAndAssertResponse(
        server,
        "ReviewsEmpireEpisodeResponse.json",
        apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST)
    ) { response -> !response.hasErrors() }
    assertResponse(
        apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.reviews).hasSize(3)
      assertThat(data.reviews?.get(2)?.stars).isEqualTo(5)
      assertThat(data.reviews?.get(2)?.commentary).isEqualTo("Amazing")
    }
    val mutation = CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput(stars = 5, commentary = Input.present("Awesome"), favoriteColor = ColorInput())
    )
    server.enqueue(mockResponse("CreateReviewResponse.json"))
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponseUpdated.json"))
    val call = apolloClient.mutate(mutation).refetchQueries(ReviewsByEpisodeQuery(Episode.EMPIRE)) as RealApolloCall<*>
    Rx2Apollo
        .from(call)
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(3)
    assertResponse(
        apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.reviews).hasSize(4)
      assertThat(data.reviews?.get(3)?.stars).isEqualTo(5)
      assertThat(data.reviews?.get(3)?.commentary).isEqualTo("Awesome")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun refetchWatchers() {
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"))
    server.enqueue(mockResponse("CreateReviewResponse.json"))
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponseUpdated.json"))
    val empireReviewsWatchResponse = AtomicReference<Response<ReviewsByEpisodeQuery.Data>>()
    val queryWatcher = apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE))
        .watcher()
        .refetchResponseFetcher(ApolloResponseFetchers.NETWORK_FIRST)
        .enqueueAndWatch(object : ApolloCall.Callback<ReviewsByEpisodeQuery.Data>() {
          override fun onResponse(response: Response<ReviewsByEpisodeQuery.Data>) {
            empireReviewsWatchResponse.set(response)
          }

          override fun onFailure(e: ApolloException) {}
        })
    val mutation = CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput(stars = 5, commentary = Input.present("Awesome"), favoriteColor = ColorInput())
    )
    Rx2Apollo
        .from(apolloClient.mutate(mutation).refetchQueries(queryWatcher.operation().name()))
        .test()
    Truth.assertThat(server.requestCount).isEqualTo(3)
    val (_, data) = empireReviewsWatchResponse.get()
    assertThat(data!!.reviews).hasSize(4)
    assertThat(data.reviews?.get(3)?.stars).isEqualTo(5)
    assertThat(data.reviews?.get(3)?.commentary).isEqualTo("Awesome")
    queryWatcher.cancel()
  }
}
