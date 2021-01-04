package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.IdFieldCacheKeyResolver
import com.apollographql.apollo.Utils.assertResponse
import com.apollographql.apollo.Utils.enqueueAndAssertResponse
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.normalizer.CreateReviewMutation
import com.apollographql.apollo.integration.normalizer.ReviewsByEpisodeQuery
import com.apollographql.apollo.integration.normalizer.type.ColorInput
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.integration.normalizer.type.ReviewInput
import com.apollographql.apollo.rx2.Rx2Apollo
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
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
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
        ReviewInput(stars = 5, commentary = Input.fromNullable("Awesome"), favoriteColor = ColorInput())
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
        apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST),
        Predicate<Response<ReviewsByEpisodeQuery.Data>> { response -> !response.hasErrors() }
    )
    assertResponse(
        apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.reviews).hasSize(3)
      assertThat(data.reviews?.get(2)?.stars).isEqualTo(5)
      assertThat(data.reviews?.get(2)?.commentary).isEqualTo("Amazing")
      true
    }
    val mutation = CreateReviewMutation(
        Episode.EMPIRE,
        ReviewInput(stars = 5, commentary = Input.fromNullable("Awesome"), favoriteColor = ColorInput())
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
        ReviewInput(stars = 5, commentary = Input.fromNullable("Awesome"), favoriteColor = ColorInput())
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
