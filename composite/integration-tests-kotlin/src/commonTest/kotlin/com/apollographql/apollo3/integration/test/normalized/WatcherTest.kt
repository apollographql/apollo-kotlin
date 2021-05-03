package com.apollographql.apollo3.integration.test.normalized

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.integration.IdFieldCacheKeyResolver
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameWithIdQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.readResource
import com.apollographql.apollo3.integration.receiveOrTimeout
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.watch
import com.apollographql.apollo3.interceptor.cache.withFetchPolicy
import com.apollographql.apollo3.interceptor.cache.withRefetchPolicy
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class WatcherTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  @BeforeTest
  fun setUp() {
    store = ApolloStore(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver)
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url()).withStore(store)
  }

  /**
   * Executing the same query out of band should update the watcher
   */
  @Test
  fun sameQueryTriggersWatcher() = runWithMainLoop {
    val query = EpisodeHeroNameQuery(Episode.EMPIRE)
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(query).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    mockServer.enqueue(readResource("EpisodeHeroNameResponseNameChange.json"))
    apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly))

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "Artoo")

    job.cancel()
  }


  /**
   * Writing to the store out of band should update the watcher
   */
  @Test
  fun storeWriteTriggersWatcher() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameWithIdQuery.Data?>()
    val operation = EpisodeHeroNameWithIdQuery(Episode.EMPIRE)
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(operation).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Someone writes to the store directly
    val data = EpisodeHeroNameWithIdQuery.Data(
        hero = EpisodeHeroNameWithIdQuery.Data.Hero(
            id = "2001",
            name = "Artoo"
        )
    )

    store.writeOperation(operation, data, CustomScalarAdapters.Empty, CacheHeaders.NONE, true)

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "Artoo")

    job.cancel()
  }

  /**
   * A new query updates the store with data that is the same as the one originally seen by the watcher
   */
  @Test
  fun noChangeSameQuery() = runWithMainLoop {
    val query = EpisodeHeroNameQuery(Episode.EMPIRE)
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(query).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Another newer call gets the same name (R2-D2)
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly))

    channel.assertEmpty()

    job.cancel()
  }

  /**
   * A new query that contains overlapping fields with the watched query should trigger the watcher
   */
  @Test
  fun differentQueryTriggersWatcher() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameWithIdQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(EpisodeHeroNameWithIdQuery(Episode.EMPIRE)).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsNameChange.json"))
    apolloClient.query(
        ApolloRequest(
            HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)
        ).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "Artoo")

    job.cancel()
  }

  /**
   * Same as noChangeSameQuery with different queries
   */
  @Test
  fun noChangeDifferentQuery() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(EpisodeHeroNameQuery(Episode.EMPIRE)).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receive()?.hero?.name, "R2-D2")

    // Another newer call gets the same information
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(
        ApolloRequest(
            HeroAndFriendsNamesWithIDsQuery(Episode.NEWHOPE)
        ).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    channel.assertEmpty()

    job.cancel()
  }

  /**
   * A test to test refetching with a NetworkOnly refetchPolicy. On every change, the watcher should get new information
   * from the network
   */
  @Test
  fun networkRefetchPolicy() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      val request = ApolloRequest(EpisodeHeroNameQuery(Episode.EMPIRE))
          .withFetchPolicy(FetchPolicy.NetworkOnly)
          .withRefetchPolicy(FetchPolicy.NetworkOnly)
      apolloClient.watch(request).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Enqueue 2 responses.
    // - The first one will be for the query just below and contains "Artoo"
    // - The second one will be for the watcher refetch and contains "ArTwo"
    mockServer.enqueue(readResource("EpisodeHeroNameResponseNameChange.json"))
    mockServer.enqueue(readResource("EpisodeHeroNameResponseNameChangeTwo.json"))
    val response = apolloClient.query(
        ApolloRequest(
            EpisodeHeroNameQuery(Episode.EMPIRE)
        ).withFetchPolicy(FetchPolicy.NetworkOnly)
    )
    assertEquals(response.data?.hero?.name, "Artoo")

    // The watcher should see "ArTwo"
    assertEquals(channel.receiveOrTimeout()?.hero?.name, "ArTwo")

    job.cancel()
  }


  @Test
  fun nothingReceivedWhenCancelled() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      val request = ApolloRequest(EpisodeHeroNameQuery(Episode.EMPIRE))
          .withFetchPolicy(FetchPolicy.NetworkOnly)
          .withRefetchPolicy(FetchPolicy.NetworkOnly)
      apolloClient.watch(request).collect {
        channel.send(it.data)
      }
    }
    job.cancelAndJoin()

    channel.assertEmpty()
  }

  private suspend fun <D> Channel<D>.assertEmpty() {
    /**
     * We might want to change the code to have something that can model emptyness in a  more precise way but
     * This should work in the very vast majority of cases
     */
    try {
      receiveOrTimeout()
      fail("Nothing should be received")
    } catch (e: TimeoutCancellationException) {
    }
  }

  /**
   * Doing the initial query as cache only will detect when the query becomes available
   */
  @Test
  fun cacheOnlyFetchPolicy() = runWithMainLoop {
    val query = EpisodeHeroNameQuery(Episode.EMPIRE)
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // This will miss as the cache should be empty
    val job = launch {
      val request = ApolloRequest(query).withFetchPolicy(FetchPolicy.CacheOnly)
      apolloClient.watch(request).collect {
        channel.send(it.data)
      }
    }

    // Another newer call gets updated information with "R2-D2"
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(ApolloRequest(query).withFetchPolicy(FetchPolicy.NetworkOnly))

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    job.cancel()
  }

  @Test
  fun queryWatcherWithCacheOnlyNeverGoesToTheNetwork() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameQuery.Data?>(capacity = Channel.UNLIMITED)
    val job = launch {
      val request = ApolloRequest(EpisodeHeroNameQuery(Episode.EMPIRE))
          .withFetchPolicy(FetchPolicy.CacheOnly)
          .withRefetchPolicy(FetchPolicy.CacheOnly)

      apolloClient.watch(request).collect {
            channel.send(it.data)
          }
    }
    
    mockServer.enqueue(readResource("StarshipByIdResponse.json"))
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))

    // execute a query that doesn't share any key with the main query
    // that will trigger a refetch that shouldn't throw
    apolloClient.query(StarshipByIdQuery("Starship1"))

    // There should be no data
    channel.assertEmpty()

    job.cancel()
  }
}
