package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.IdCacheResolver
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameWithIdQuery
import com.apollographql.apollo3.integration.normalizer.ReviewsByEpisodeQuery
import com.apollographql.apollo3.integration.normalizer.UpdateReviewMutation
import com.apollographql.apollo3.integration.normalizer.type.ColorInput
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.normalizer.type.ReviewInput
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.watch
import com.apollographql.apollo3.interceptor.cache.withFetchPolicy
import com.apollographql.apollo3.interceptor.cache.withOptimisticUpdates
import com.apollographql.apollo3.interceptor.cache.withRefetchPolicy
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.receiveOrTimeout
import com.apollographql.apollo3.testing.runWithMainLoop
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import readResource
import kotlin.test.BeforeTest
import kotlin.test.Test
import assertEquals2 as assertEquals

class OptimisticCacheTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdCacheResolver())
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url()).withStore(store)
  }


  /**
   * Write the updates programmatically, make sure they are seen,
   * roll them back, make sure we're back to the initial state
   */
  @Test
  fun programmaticOptimiticUpdates() = runWithMainLoop {
    val query = HeroAndFriendsNamesQuery(Episode.JEDI)

    mockServer.enqueue(readResource("HeroAndFriendsNameResponse.json"))
    apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly))

    val mutationId = uuid4()
    val data = HeroAndFriendsNamesQuery.Data(HeroAndFriendsNamesQuery.Data.Hero(
        "R222-D222",
        listOf(
            HeroAndFriendsNamesQuery.Data.Hero.Friend(
                "SuperMan"
            ),
            HeroAndFriendsNamesQuery.Data.Hero.Friend(
                "Batman"
            )
        )
    ))
    store.writeOptimisticUpdates(
        operation = query,
        operationData = data,
        mutationId = mutationId,
        publish = true)

    var response = apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))

    assertEquals(response.data?.hero?.name, "R222-D222")
    assertEquals(response.data?.hero?.friends?.size, 2)
    assertEquals(response.data?.hero?.friends?.get(0)?.name, "SuperMan")
    assertEquals(response.data?.hero?.friends?.get(1)?.name, "Batman")

    store.rollbackOptimisticUpdates(mutationId, false)
    response = apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly))

    assertEquals(response.data?.hero?.name, "R2-D2")
    assertEquals(response.data?.hero?.friends?.size, 3)
    assertEquals(response.data?.hero?.friends?.get(0)?.name, "Luke Skywalker")
    assertEquals(response.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals(response.data?.hero?.friends?.get(2)?.name, "Leia Organa")
  }

  /**
   * A more complex scenario where we stack optimistic updates
   */
  @Test
  fun two_optimistic_two_rollback() = runWithMainLoop {
    val query1 = HeroAndFriendsNamesWithIDsQuery(Episode.JEDI)
    val mutationId1 = uuid4()

    // execute query1 from the network
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.NetworkOnly))

    // now write some optimistic updates for query1
    val data1 = HeroAndFriendsNamesWithIDsQuery.Data(
        HeroAndFriendsNamesWithIDsQuery.Data.Hero(
            "2001",
            "R222-D222",
            listOf(
                HeroAndFriendsNamesWithIDsQuery.Data.Hero.Friend(
                    "1000",
                    "SuperMan"
                ),
                HeroAndFriendsNamesWithIDsQuery.Data.Hero.Friend(
                    "1003",
                    "Batman"
                )
            )
        )
    )
    store.writeOptimisticUpdates(
        operation = query1,
        operationData = data1,
        mutationId = mutationId1,
        publish = true)

    // check if query1 see optimistic updates
    var response1 = apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "SuperMan")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")

    // execute query2
    val query2 = HeroNameWithIdQuery()
    val mutationId2 = uuid4()

    mockServer.enqueue(readResource("HeroNameWithIdResponse.json"))
    apolloClient.query(query2)

    // write optimistic data2
    val data2 = HeroNameWithIdQuery.Data(HeroNameWithIdQuery.Data.Hero(
        "1000",
        "Beast"
    ))
    store.writeOptimisticUpdates(
        operation = query2,
        operationData = data2,
        mutationId = mutationId2,
        publish = true)

    // check if query1 sees data2
    response1 = apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Beast")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")

    // check if query2 sees data2
    var response2 = apolloClient.query(ApolloRequest(query2).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Beast")

    // rollback data1
    store.rollbackOptimisticUpdates(mutationId1, false)

    // check if query2 sees the rollback
    response1 = apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R2-D2")
    assertEquals(response1.data?.hero?.friends?.size, 3)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Beast")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1002")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals(response1.data?.hero?.friends?.get(2)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(2)?.name, "Leia Organa")

    // check if query2 see the latest optimistic updates
    response2 = apolloClient.query(ApolloRequest(query2).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Beast")

    // rollback query2 optimistic updates
    store.rollbackOptimisticUpdates(mutationId2, false)

    // check if query2 see the latest optimistic updates
    response2 = apolloClient.query(ApolloRequest(query2).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "SuperMan")
  }

  @Test
  fun mutation_and_query_watcher() = runWithMainLoop {
    mockServer.enqueue(readResource("ReviewsEmpireEpisodeResponse.json"))
    val channel = Channel<ReviewsByEpisodeQuery.Data?>()
    val job = launch {
      apolloClient.watch(
          ApolloRequest(ReviewsByEpisodeQuery(Episode.EMPIRE))
              .withFetchPolicy(FetchPolicy.NetworkOnly)
              .withRefetchPolicy(FetchPolicy.CacheOnly)
      ).collect {
        channel.send(it.data)
      }
    }

    var watcherData = channel.receive()

    // before mutation and optimistic updates
    assertEquals(watcherData?.reviews?.size, 3)
    assertEquals(watcherData?.reviews?.get(0)?.id, "empireReview1")
    assertEquals(watcherData?.reviews?.get(0)?.stars, 1)
    assertEquals(watcherData?.reviews?.get(0)?.commentary, "Boring")
    assertEquals(watcherData?.reviews?.get(1)?.id, "empireReview2")
    assertEquals(watcherData?.reviews?.get(1)?.stars, 2)
    assertEquals(watcherData?.reviews?.get(1)?.commentary, "So-so")
    assertEquals(watcherData?.reviews?.get(2)?.id, "empireReview3")
    assertEquals(watcherData?.reviews?.get(2)?.stars, 5)
    assertEquals(watcherData?.reviews?.get(2)?.commentary, "Amazing")

    /**
     * There is a small potential for a race condition here. The changedKeys event from the optimistic updates might
     * be received after the network response has been written and therefore the refetch will see the new data right ahead.
     *
     * To limit the occurence of this happening, we introduce a small delay in the network response here.
     */
    mockServer.enqueue(readResource("UpdateReviewResponse.json"), 100)
    val updateReviewMutation = UpdateReviewMutation(
        "empireReview2",
        ReviewInput(
            commentary = Optional.Present("Not Bad"),
            stars = 4,
            favoriteColor = ColorInput()
        )
    )
    apolloClient.mutate(
        ApolloRequest(updateReviewMutation).withOptimisticUpdates(
            UpdateReviewMutation.Data(
                UpdateReviewMutation.Data.UpdateReview(
                    "empireReview2",
                    5,
                    "Great"
                )
            )
        )
    )

    /**
     * optimistic updates
     */
    watcherData = channel.receive()
    assertEquals(watcherData?.reviews?.size, 3)
    assertEquals(watcherData?.reviews?.get(0)?.id, "empireReview1")
    assertEquals(watcherData?.reviews?.get(0)?.stars, 1)
    assertEquals(watcherData?.reviews?.get(0)?.commentary, "Boring")
    assertEquals(watcherData?.reviews?.get(1)?.id, "empireReview2")
    assertEquals(watcherData?.reviews?.get(1)?.stars, 5)
    assertEquals(watcherData?.reviews?.get(1)?.commentary, "Great")
    assertEquals(watcherData?.reviews?.get(2)?.id, "empireReview3")
    assertEquals(watcherData?.reviews?.get(2)?.stars, 5)
    assertEquals(watcherData?.reviews?.get(2)?.commentary, "Amazing")

    // after mutation with rolled back optimistic updates
    watcherData = channel.receiveOrTimeout()
    assertEquals(watcherData?.reviews?.size, 3)
    assertEquals(watcherData?.reviews?.get(0)?.id, "empireReview1")
    assertEquals(watcherData?.reviews?.get(0)?.stars, 1)
    assertEquals(watcherData?.reviews?.get(0)?.commentary, "Boring")
    assertEquals(watcherData?.reviews?.get(1)?.id, "empireReview2")
    assertEquals(watcherData?.reviews?.get(1)?.stars, 4)
    assertEquals(watcherData?.reviews?.get(1)?.commentary, "Not Bad")
    assertEquals(watcherData?.reviews?.get(2)?.id, "empireReview3")
    assertEquals(watcherData?.reviews?.get(2)?.stars, 5)
    assertEquals(watcherData?.reviews?.get(2)?.commentary, "Amazing")

    job.cancel()
  }

  @Test
  @Throws(Exception::class)
  fun two_optimistic_reverse_rollback_order() = runWithMainLoop {
    val query1 = HeroAndFriendsNamesWithIDsQuery(Episode.JEDI)
    val mutationId1 = uuid4()
    val query2 = HeroNameWithIdQuery()
    val mutationId2 = uuid4()

    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(query1)

    mockServer.enqueue(readResource("HeroNameWithIdResponse.json"))
    apolloClient.query(query2)

    val data1 = HeroAndFriendsNamesWithIDsQuery.Data(
        HeroAndFriendsNamesWithIDsQuery.Data.Hero(
            "2001",
            "R222-D222",
            listOf(
                HeroAndFriendsNamesWithIDsQuery.Data.Hero.Friend(
                    "1000",
                    "Robocop"
                ),
                HeroAndFriendsNamesWithIDsQuery.Data.Hero.Friend(
                    "1003",
                    "Batman"
                )
            )
        )
    )
    store.writeOptimisticUpdates(
        operation = query1,
        operationData = data1,
        mutationId = mutationId1,
        publish = true)
    val data2 = HeroNameWithIdQuery.Data(HeroNameWithIdQuery.Data.Hero(
        "1000",
        "Spiderman"
    ))
    store.writeOptimisticUpdates(
        operation = query2,
        operationData = data2,
        mutationId = mutationId2,
        publish = true)

    // check if query1 see optimistic updates
    var response1 = apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Spiderman")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")


    // check if query2 see the latest optimistic updates
    var response2 = apolloClient.query(ApolloRequest(query2).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Spiderman")

    // rollback query2 optimistic updates
    store.rollbackOptimisticUpdates(mutationId2, false)

    // check if query1 see the latest optimistic updates
    response1 = apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Robocop")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")


    // check if query2 see the latest optimistic updates
    response2 = apolloClient.query(ApolloRequest(query2).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Robocop")

    // rollback query1 optimistic updates
    store.rollbackOptimisticUpdates(mutationId1, false)

    // check if query1 see the latest non-optimistic updates
    response1 = apolloClient.query(ApolloRequest(query1).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R2-D2")
    assertEquals(response1.data?.hero?.friends?.size, 3)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "SuperMan")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1002")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Han Solo")
    assertEquals(response1.data?.hero?.friends?.get(2)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(2)?.name, "Leia Organa")


    // check if query2 see the latest non-optimistic updates
    response2 = apolloClient.query(ApolloRequest(query2).withFetchPolicy(FetchPolicy.CacheOnly))
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "SuperMan")
  }
}
