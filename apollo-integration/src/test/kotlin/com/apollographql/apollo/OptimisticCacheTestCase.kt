package com.apollographql.apollo

import com.apollographql.apollo.Utils.assertResponse
import com.apollographql.apollo.Utils.enqueueAndAssertResponse
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.normalizer.*
import com.apollographql.apollo.integration.normalizer.UpdateReviewMutation.UpdateReview
import com.apollographql.apollo.integration.normalizer.type.ColorInput
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.integration.normalizer.type.ReviewInput
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.*

class OptimisticCacheTestCase {
  private var apolloClient: ApolloClient? = null

  @Rule
  val server = MockWebServer()
  @Before
  fun setUp() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .dispatcher(immediateExecutor())
        .build()
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }

  @Test
  @Throws(Exception::class)
  fun simple() {
    val query = HeroAndFriendsNamesQuery(fromNullable(Episode.JEDI))
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameResponse.json",
        apolloClient!!.query(query),
        Predicate<Response<HeroAndFriendsNamesQuery.Data?>> { response -> !response.hasErrors() }
    )
    val mutationId = UUID.randomUUID()
    val data = HeroAndFriendsNamesQuery.Data(HeroAndFriendsNamesQuery.Hero(
        "Droid",
        "R222-D222",
        Arrays.asList(
            HeroAndFriendsNamesQuery.Friend(
                "Human",
                "SuperMan"
            ),
            HeroAndFriendsNamesQuery.Friend(
                "Human",
                "Batman"
            )
        )
    ))
    apolloClient!!.apolloStore.writeOptimisticUpdatesAndPublish(query, data, mutationId).execute()
    assertResponse(
        apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data1) ->
      assertThat(data1!!.hero?.name).isEqualTo("R222-D222")
      assertThat(data1!!.hero?.friends).hasSize(2)
      assertThat(data1!!.hero?.friends?.get(0)?.name).isEqualTo("SuperMan")
      assertThat(data1!!.hero?.friends?.get(1)?.name).isEqualTo("Batman")
      true
    }
    apolloClient!!.apolloStore.rollbackOptimisticUpdates(mutationId).execute()
    assertResponse(
        apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data1) ->
      assertThat(data1!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data1!!.hero?.friends).hasSize(3)
      assertThat(data1!!.hero?.friends?.get(0)?.name).isEqualTo("Luke Skywalker")
      assertThat(data1!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(data1!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun two_optimistic_two_rollback() {
    val query1 = HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.JEDI))
    val mutationId1 = UUID.randomUUID()
    val query2 = HeroNameWithIdQuery()
    val mutationId2 = UUID.randomUUID()
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient!!.query(query1),
        Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data?>> { response -> !response.hasErrors() }
    )
    val data1 = HeroAndFriendsNamesWithIDsQuery.Data(
        HeroAndFriendsNamesWithIDsQuery.Hero(
            "Droid",
            "2001",
            "R222-D222",
            Arrays.asList(
                HeroAndFriendsNamesWithIDsQuery.Friend(
                    "Human",
                    "1000",
                    "SuperMan"
                ),
                HeroAndFriendsNamesWithIDsQuery.Friend(
                    "Human",
                    "1003",
                    "Batman"
                )
            )
        )
    )
    apolloClient!!.apolloStore.writeOptimisticUpdatesAndPublish(query1, data1, mutationId1).execute()

    // check if query1 see optimistic updates
    assertResponse(
        apolloClient!!.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data!!.hero?.name).isEqualTo("R222-D222")
      assertThat(data!!.hero?.friends).hasSize(2)
      assertThat(data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data!!.hero?.friends?.get(0)?.name).isEqualTo("SuperMan")
      assertThat(data!!.hero?.friends?.get(1)?.id).isEqualTo("1003")
      assertThat(data!!.hero?.friends?.get(1)?.name).isEqualTo("Batman")
      true
    }
    enqueueAndAssertResponse(
        server,
        "HeroNameWithIdResponse.json",
        apolloClient!!.query(query2),
        Predicate<Response<HeroNameWithIdQuery.Data?>> { response -> !response.hasErrors() }
    )
    val data2 = HeroNameWithIdQuery.Data(HeroNameWithIdQuery.Hero(
        "Human",
        "1000",
        "Beast"
    ))
    apolloClient!!.apolloStore.writeOptimisticUpdatesAndPublish(query2, data2, mutationId2).execute()

    // check if query1 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data!!.hero?.name).isEqualTo("R222-D222")
      assertThat(data!!.hero?.friends).hasSize(2)
      assertThat(data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data!!.hero?.friends?.get(0)?.name).isEqualTo("Beast")
      assertThat(data!!.hero?.friends?.get(1)?.id).isEqualTo("1003")
      assertThat(data!!.hero?.friends?.get(1)?.name).isEqualTo("Batman")
      true
    }

    // check if query2 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("1000")
      assertThat(data!!.hero?.name).isEqualTo("Beast")
      true
    }

    // rollback query1 optimistic updates
    apolloClient!!.apolloStore.rollbackOptimisticUpdates(mutationId1).execute()

    // check if query1 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data!!.hero?.friends).hasSize(3)
      assertThat(data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data!!.hero?.friends?.get(0)?.name).isEqualTo("Beast")
      assertThat(data!!.hero?.friends?.get(1)?.id).isEqualTo("1002")
      assertThat(data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(data!!.hero?.friends?.get(2)?.id).isEqualTo("1003")
      assertThat(data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }

    // check if query2 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("1000")
      assertThat(data!!.hero?.name).isEqualTo("Beast")
      true
    }

    // rollback query2 optimistic updates
    apolloClient!!.apolloStore.rollbackOptimisticUpdates(mutationId2).execute()

    // check if query2 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("1000")
      assertThat(data!!.hero?.name).isEqualTo("SuperMan")
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun full_persisted_partial_optimistic() {
    enqueueAndAssertResponse(
        server,
        "HeroNameWithEnumsResponse.json",
        apolloClient!!.query(HeroNameWithEnumsQuery()),
        Predicate<Response<HeroNameWithEnumsQuery.Data?>> { response -> !response.hasErrors() }
    )
    val mutationId = UUID.randomUUID()
    apolloClient!!.apolloStore.writeOptimisticUpdates(
        HeroNameQuery(),
        HeroNameQuery.Data(HeroNameQuery.Hero("Droid", "R22-D22")),
        mutationId
    ).execute()
    assertResponse(
        apolloClient!!.query(HeroNameWithEnumsQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R22-D22")
      assertThat(data!!.hero?.firstAppearsIn).isEqualTo(Episode.EMPIRE)
      assertThat(data!!.hero?.appearsIn).isEqualTo(Arrays.asList(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI))
      true
    }
    apolloClient!!.apolloStore.rollbackOptimisticUpdates(mutationId).execute()
    assertResponse(
        apolloClient!!.query(HeroNameWithEnumsQuery()).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data!!.hero?.firstAppearsIn).isEqualTo(Episode.EMPIRE)
      assertThat(data!!.hero?.appearsIn).isEqualTo(Arrays.asList(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI))
      true
    }
  }

  @Test
  @Throws(Exception::class)
  fun mutation_and_query_watcher() {
    server.enqueue(mockResponse("ReviewsEmpireEpisodeResponse.json"))
    val watcherData: MutableList<ReviewsByEpisodeQuery.Data?> = ArrayList()
    apolloClient!!.query(ReviewsByEpisodeQuery(Episode.EMPIRE)).responseFetcher(ApolloResponseFetchers.NETWORK_FIRST)
        .watcher().refetchResponseFetcher(ApolloResponseFetchers.CACHE_FIRST)
        .enqueueAndWatch(object : ApolloCall.Callback<ReviewsByEpisodeQuery.Data>() {
          override fun onResponse(response: Response<ReviewsByEpisodeQuery.Data>) {
            watcherData.add(response.data())
          }

          override fun onFailure(e: ApolloException) {}
        })
    server.enqueue(mockResponse("UpdateReviewResponse.json"))
    val updateReviewMutation = UpdateReviewMutation(
        "empireReview2",
        ReviewInput(
            commentary = Input.fromNullable("Great"),
            stars = 5,
            favoriteColor = ColorInput()
        )
    )
    apolloClient!!.mutate(updateReviewMutation, UpdateReviewMutation.Data(UpdateReview(
        "Review", "empireReview2", 5, "Great"))).enqueue(
        object : ApolloCall.Callback<UpdateReviewMutation.Data>() {
          override fun onResponse(response: Response<UpdateReviewMutation.Data>) {}
          override fun onFailure(e: ApolloException) {}
        }
    )
    Truth.assertThat(watcherData).hasSize(3)

    // before mutation and optimistic updates
    assertThat(watcherData[0]!!.reviews).hasSize(3)
    assertThat(watcherData[0]!!.reviews?.get(0)?.id).isEqualTo("empireReview1")
    assertThat(watcherData[0]!!.reviews?.get(0)?.stars).isEqualTo(1)
    assertThat(watcherData[0]!!.reviews?.get(0)?.commentary).isEqualTo("Boring")
    assertThat(watcherData[0]!!.reviews?.get(1)?.id).isEqualTo("empireReview2")
    assertThat(watcherData[0]!!.reviews?.get(1)?.stars).isEqualTo(2)
    assertThat(watcherData[0]!!.reviews?.get(1)?.commentary).isEqualTo("So-so")
    assertThat(watcherData[0]!!.reviews?.get(2)?.id).isEqualTo("empireReview3")
    assertThat(watcherData[0]!!.reviews?.get(2)?.stars).isEqualTo(5)
    assertThat(watcherData[0]!!.reviews?.get(2)?.commentary).isEqualTo("Amazing")

    // optimistic updates
    assertThat(watcherData[1]!!.reviews).hasSize(3)
    assertThat(watcherData[1]!!.reviews?.get(0)?.id).isEqualTo("empireReview1")
    assertThat(watcherData[1]!!.reviews?.get(0)?.stars).isEqualTo(1)
    assertThat(watcherData[1]!!.reviews?.get(0)?.commentary).isEqualTo("Boring")
    assertThat(watcherData[1]!!.reviews?.get(1)?.id).isEqualTo("empireReview2")
    assertThat(watcherData[1]!!.reviews?.get(1)?.stars).isEqualTo(5)
    assertThat(watcherData[1]!!.reviews?.get(1)?.commentary).isEqualTo("Great")
    assertThat(watcherData[1]!!.reviews?.get(2)?.id).isEqualTo("empireReview3")
    assertThat(watcherData[1]!!.reviews?.get(2)?.stars).isEqualTo(5)
    assertThat(watcherData[1]!!.reviews?.get(2)?.commentary).isEqualTo("Amazing")

    // after mutation with rolled back optimistic updates
    assertThat(watcherData[2]!!.reviews).hasSize(3)
    assertThat(watcherData[2]!!.reviews?.get(0)?.id).isEqualTo("empireReview1")
    assertThat(watcherData[2]!!.reviews?.get(0)?.stars).isEqualTo(1)
    assertThat(watcherData[2]!!.reviews?.get(0)?.commentary).isEqualTo("Boring")
    assertThat(watcherData[2]!!.reviews?.get(1)?.id).isEqualTo("empireReview2")
    assertThat(watcherData[2]!!.reviews?.get(1)?.stars).isEqualTo(4)
    assertThat(watcherData[2]!!.reviews?.get(1)?.commentary).isEqualTo("Not Bad")
    assertThat(watcherData[2]!!.reviews?.get(2)?.id).isEqualTo("empireReview3")
    assertThat(watcherData[2]!!.reviews?.get(2)?.stars).isEqualTo(5)
    assertThat(watcherData[2]!!.reviews?.get(2)?.commentary).isEqualTo("Amazing")
  }

  @Test
  @Throws(Exception::class)
  fun two_optimistic_reverse_rollback_order() {
    val query1 = HeroAndFriendsNamesWithIDsQuery(fromNullable(Episode.JEDI))
    val mutationId1 = UUID.randomUUID()
    val query2 = HeroNameWithIdQuery()
    val mutationId2 = UUID.randomUUID()
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsResponse.json",
        apolloClient!!.query(query1),
        Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data?>> { response -> !response.hasErrors() }
    )
    enqueueAndAssertResponse(
        server,
        "HeroNameWithIdResponse.json",
        apolloClient!!.query(query2),
        Predicate<Response<HeroNameWithIdQuery.Data?>> { response -> !response.hasErrors() }
    )
    val data1 = HeroAndFriendsNamesWithIDsQuery.Data(
        HeroAndFriendsNamesWithIDsQuery.Hero(
            "Droid",
            "2001",
            "R222-D222",
            listOf(
                HeroAndFriendsNamesWithIDsQuery.Friend(
                    "Human",
                    "1000",
                    "Robocop"
                ),
                HeroAndFriendsNamesWithIDsQuery.Friend(
                    "Human",
                    "1003",
                    "Batman"
                )
            )
        )
    )
    apolloClient!!.apolloStore.writeOptimisticUpdatesAndPublish(query1, data1, mutationId1).execute()
    val data2 = HeroNameWithIdQuery.Data(HeroNameWithIdQuery.Hero(
        "Human",
        "1000",
        "Spiderman"
    ))
    apolloClient!!.apolloStore.writeOptimisticUpdatesAndPublish(query2, data2, mutationId2).execute()

    // check if query1 see optimistic updates
    assertResponse(
        apolloClient!!.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data!!.hero?.name).isEqualTo("R222-D222")
      assertThat(data!!.hero?.friends).hasSize(2)
      assertThat(data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data!!.hero?.friends?.get(0)?.name).isEqualTo("Spiderman")
      assertThat(data!!.hero?.friends?.get(1)?.id).isEqualTo("1003")
      assertThat(data!!.hero?.friends?.get(1)?.name).isEqualTo("Batman")
      true
    }

    // check if query2 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("1000")
      assertThat(data!!.hero?.name).isEqualTo("Spiderman")
      true
    }

    // rollback query2 optimistic updates
    apolloClient!!.apolloStore.rollbackOptimisticUpdates(mutationId2).execute()

    // check if query1 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data!!.hero?.name).isEqualTo("R222-D222")
      assertThat(data!!.hero?.friends).hasSize(2)
      assertThat(data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data!!.hero?.friends?.get(0)?.name).isEqualTo("Robocop")
      assertThat(data!!.hero?.friends?.get(1)?.id).isEqualTo("1003")
      assertThat(data!!.hero?.friends?.get(1)?.name).isEqualTo("Batman")
      true
    }

    // check if query2 see the latest optimistic updates
    assertResponse(
        apolloClient!!.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("1000")
      assertThat(data!!.hero?.name).isEqualTo("Robocop")
      true
    }

    // rollback query1 optimistic updates
    apolloClient!!.apolloStore.rollbackOptimisticUpdates(mutationId1).execute()

    // check if query1 see the latest non-optimistic updates
    assertResponse(
        apolloClient!!.query(query1).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("2001")
      assertThat(data!!.hero?.name).isEqualTo("R2-D2")
      assertThat(data!!.hero?.friends).hasSize(3)
      assertThat(data!!.hero?.friends?.get(0)?.id).isEqualTo("1000")
      assertThat(data!!.hero?.friends?.get(0)?.name).isEqualTo("SuperMan")
      assertThat(data!!.hero?.friends?.get(1)?.id).isEqualTo("1002")
      assertThat(data!!.hero?.friends?.get(1)?.name).isEqualTo("Han Solo")
      assertThat(data!!.hero?.friends?.get(2)?.id).isEqualTo("1003")
      assertThat(data!!.hero?.friends?.get(2)?.name).isEqualTo("Leia Organa")
      true
    }

    // check if query2 see the latest non-optimistic updates
    assertResponse(
        apolloClient!!.query(query2).responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
    ) { (_, data) ->
      assertThat(data!!.hero?.id).isEqualTo("1000")
      assertThat(data!!.hero?.name).isEqualTo("SuperMan")
      true
    }
  }
}