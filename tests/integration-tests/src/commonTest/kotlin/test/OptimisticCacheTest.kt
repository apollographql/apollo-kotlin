package test

import IdCacheKeyGenerator
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.optimisticUpdates
import com.apollographql.apollo.cache.normalized.refetchPolicy
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.cache.normalized.watch
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.HeroNameWithIdQuery
import com.apollographql.apollo.integration.normalizer.ReviewsByEpisodeQuery
import com.apollographql.apollo.integration.normalizer.UpdateReviewMutation
import com.apollographql.apollo.integration.normalizer.type.ColorInput
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.integration.normalizer.type.ReviewInput
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.awaitElement
import com.apollographql.apollo.testing.internal.runTest
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import testFixtureToUtf8
import kotlin.test.Test
import assertEquals2 as assertEquals

class OptimisticCacheTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), cacheKeyGenerator = IdCacheKeyGenerator)
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private suspend fun tearDown() {
    mockServer.close()
  }

  /**
   * Write the updates programmatically, make sure they are seen,
   * roll them back, make sure we're back to the initial state
   */
  @Test
  fun programmaticOptimiticUpdates() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroAndFriendsNamesQuery(Episode.JEDI)

    mockServer.enqueueString(testFixtureToUtf8("HeroAndFriendsNameResponse.json"))
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    val mutationId = uuid4()
    val data = HeroAndFriendsNamesQuery.Data(HeroAndFriendsNamesQuery.Hero(
        "R222-D222",
        listOf(
            HeroAndFriendsNamesQuery.Friend(
                "SuperMan"
            ),
            HeroAndFriendsNamesQuery.Friend(
                "Batman"
            )
        )
    ))
    store.writeOptimisticUpdates(
        operation = query,
        operationData = data,
        mutationId = mutationId,
        publish = true)

    var response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()

    assertEquals(response.data?.hero?.name, "R222-D222")
    assertEquals(response.data?.hero?.friends?.size, 2)
    assertEquals(response.data?.hero?.friends?.get(0)?.name, "SuperMan")
    assertEquals(response.data?.hero?.friends?.get(1)?.name, "Batman")

    store.rollbackOptimisticUpdates(mutationId, false)
    response = apolloClient.query(query).fetchPolicy(FetchPolicy.CacheOnly).execute()

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
  fun two_optimistic_two_rollback() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query1 = HeroAndFriendsNamesWithIDsQuery(Episode.JEDI)
    val mutationId1 = uuid4()

    // execute query1 from the network
    mockServer.enqueueString(testFixtureToUtf8("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(query1).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    // now write some optimistic updates for query1
    val data1 = HeroAndFriendsNamesWithIDsQuery.Data(
        HeroAndFriendsNamesWithIDsQuery.Hero(
            "2001",
            "R222-D222",
            listOf(
                HeroAndFriendsNamesWithIDsQuery.Friend(
                    "1000",
                    "SuperMan"
                ),
                HeroAndFriendsNamesWithIDsQuery.Friend(
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
    var response1 = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheOnly).execute()
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

    mockServer.enqueueString(testFixtureToUtf8("HeroNameWithIdResponse.json"))
    apolloClient.query(query2).execute()

    // write optimistic data2
    val data2 = HeroNameWithIdQuery.Data(HeroNameWithIdQuery.Hero(
        "1000",
        "Beast"
    ))
    store.writeOptimisticUpdates(
        operation = query2,
        operationData = data2,
        mutationId = mutationId2,
        publish = true)

    // check if query1 sees data2
    response1 = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Beast")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")

    // check if query2 sees data2
    var response2 = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Beast")

    // rollback data1
    store.rollbackOptimisticUpdates(mutationId1, false)

    // check if query2 sees the rollback
    response1 = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheOnly).execute()
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
    response2 = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Beast")

    // rollback query2 optimistic updates
    store.rollbackOptimisticUpdates(mutationId2, false)

    // check if query2 see the latest optimistic updates
    response2 = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "SuperMan")
  }

  @Test
  fun mutation_and_query_watcher() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("ReviewsEmpireEpisodeResponse.json"))
    val channel = Channel<ReviewsByEpisodeQuery.Data?>()
    val job = launch {
      apolloClient.query(ReviewsByEpisodeQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .refetchPolicy(FetchPolicy.CacheOnly)
          .watch()
          .collect {
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
    mockServer.enqueueString(testFixtureToUtf8("UpdateReviewResponse.json"), 100)
    val updateReviewMutation = UpdateReviewMutation(
        "empireReview2",
        ReviewInput(
            4,
            Optional.Present("Not Bad"),
            ColorInput(
                Optional.Absent,
                Optional.Absent,
                Optional.Absent
            )
        )
    )
    apolloClient.mutation(updateReviewMutation).optimisticUpdates(
        UpdateReviewMutation.Data(
            UpdateReviewMutation.UpdateReview(
                "empireReview2",
                5,
                "Great"
            )
        )
    ).execute()

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
    @Suppress("DEPRECATION")
    watcherData = channel.awaitElement()
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
  fun two_optimistic_reverse_rollback_order() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query1 = HeroAndFriendsNamesWithIDsQuery(Episode.JEDI)
    val mutationId1 = uuid4()
    val query2 = HeroNameWithIdQuery()
    val mutationId2 = uuid4()

    mockServer.enqueueString(testFixtureToUtf8("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(query1).execute()

    mockServer.enqueueString(testFixtureToUtf8("HeroNameWithIdResponse.json"))
    apolloClient.query(query2).execute()

    val data1 = HeroAndFriendsNamesWithIDsQuery.Data(
        HeroAndFriendsNamesWithIDsQuery.Hero(
            "2001",
            "R222-D222",
            listOf(
                HeroAndFriendsNamesWithIDsQuery.Friend(
                    "1000",
                    "Robocop"
                ),
                HeroAndFriendsNamesWithIDsQuery.Friend(
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
    val data2 = HeroNameWithIdQuery.Data(HeroNameWithIdQuery.Hero(
        "1000",
        "Spiderman"
    ))
    store.writeOptimisticUpdates(
        operation = query2,
        operationData = data2,
        mutationId = mutationId2,
        publish = true)

    // check if query1 see optimistic updates
    var response1 = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Spiderman")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")


    // check if query2 see the latest optimistic updates
    var response2 = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Spiderman")

    // rollback query2 optimistic updates
    store.rollbackOptimisticUpdates(mutationId2, false)

    // check if query1 see the latest optimistic updates
    response1 = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response1.data?.hero?.id, "2001")
    assertEquals(response1.data?.hero?.name, "R222-D222")
    assertEquals(response1.data?.hero?.friends?.size, 2)
    assertEquals(response1.data?.hero?.friends?.get(0)?.id, "1000")
    assertEquals(response1.data?.hero?.friends?.get(0)?.name, "Robocop")
    assertEquals(response1.data?.hero?.friends?.get(1)?.id, "1003")
    assertEquals(response1.data?.hero?.friends?.get(1)?.name, "Batman")


    // check if query2 see the latest optimistic updates
    response2 = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "Robocop")

    // rollback query1 optimistic updates
    store.rollbackOptimisticUpdates(mutationId1, false)

    // check if query1 see the latest non-optimistic updates
    response1 = apolloClient.query(query1).fetchPolicy(FetchPolicy.CacheOnly).execute()
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
    response2 = apolloClient.query(query2).fetchPolicy(FetchPolicy.CacheOnly).execute()
    assertEquals(response2.data?.hero?.id, "1000")
    assertEquals(response2.data?.hero?.name, "SuperMan")
  }
}
