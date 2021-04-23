package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.ApolloInternal
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.ApolloStore
import com.apollographql.apollo3.integration.IdFieldCacheKeyResolver
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameWithIdQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.integration.readResource
import com.apollographql.apollo3.integration.receiveOrTimeout
import com.apollographql.apollo3.interceptor.cache.FetchPolicy
import com.apollographql.apollo3.interceptor.cache.normalizedCache
import com.apollographql.apollo3.interceptor.cache.watch
import com.apollographql.apollo3.interceptor.cache.withFetchPolicy
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.TimeoutCancellationException
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
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .normalizedCache(store)
        .build()
  }

  @Test
  fun aNewQueryChangingTheNameTriggersTheWatcher() = runWithMainLoop {
    val query = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))
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


  @Test
  fun writingANewNameToTheStoreTriggersTheWatcher() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameWithIdQuery.Data?>()
    val operation = EpisodeHeroNameWithIdQuery(Input.Present(Episode.EMPIRE))
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

    store.writeOperation(operation, data, ResponseAdapterCache.DEFAULT, CacheHeaders.NONE, true)

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "Artoo")

    job.cancel()
  }

  @Test
  fun ifTheDataDidNotChangeTheWatcherIsNotTriggered() = runWithMainLoop {
    val query = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))
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

    try {
      channel.receiveOrTimeout()
      fail("Nothing should be received")
    } catch (e: TimeoutCancellationException) {
    }

    job.cancel()
  }

  @Test
  fun aDifferentQueryCanAlsoTriggerTheWatcher() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameWithIdQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(EpisodeHeroNameWithIdQuery(Input.Present(Episode.EMPIRE))).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsNameChange.json"))
    apolloClient.query(
        ApolloRequest(
            HeroAndFriendsNamesWithIDsQuery(Input.Present(Episode.NEWHOPE))
        ).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    assertEquals(channel.receiveOrTimeout()?.hero?.name, "Artoo")

    job.cancel()
  }

  @Test
  fun testQueryWatcherNotUpdated_DifferentQueries() = runWithMainLoop {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    // The first query should get a "R2-D2" name
    mockServer.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.watch(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))).collect {
        channel.send(it.data)
      }
    }

    assertEquals(channel.receive()?.hero?.name, "R2-D2")

    // Another newer call gets the same information
    mockServer.enqueue(readResource("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(
        ApolloRequest(
            HeroAndFriendsNamesWithIDsQuery(Input.Present(Episode.NEWHOPE))
        ).withFetchPolicy(FetchPolicy.NetworkOnly)
    )

    try {
      channel.receiveOrTimeout()
      fail("Nothing should be received")
    } catch (e: TimeoutCancellationException) {
    }

    job.cancel()
  }
//
//  @Test
//  fun testRefetchCacheControl() {
//    val heroNameList: MutableList<String> = ArrayList()
//    server.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
//    val query = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))
//    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
//    watcher.refetchResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY) //Force network instead of CACHE_FIRST default
//        .enqueueAndWatch(
//            object : Callback<EpisodeHeroNameQuery.Data>() {
//              override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {
//                heroNameList.add(response.data!!.hero!!.name)
//              }
//
//              override fun onFailure(e: ApolloException) {
//                Assert.fail(e.message)
//              }
//            })
//
//    //A different call gets updated information.
//    server.enqueue(readResource("EpisodeHeroNameResponseNameChange.json"))
//
//    //To verify that the updated response comes from server use a different name change
//    // -- this is for the refetch
//    server.enqueue(readResource("EpisodeHeroNameResponseNameChangeTwo.json"))
//    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
//    watcher.cancel()
//    assertThat(heroNameList[0]).isEqualTo("R2-D2")
//    assertThat(heroNameList[1]).isEqualTo("ArTwo")
//    assertThat(heroNameList.size).isEqualTo(2)
//  }
//
//  @Test
//  fun testQueryWatcherUpdated_SameQuery_DifferentResults_cacheOnly() {
//    val heroNameList: MutableList<String> = ArrayList()
//    val query = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))
//    server.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
//    apolloClient.query(query).enqueue(object : Callback<EpisodeHeroNameQuery.Data>() {
//      override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {}
//      override fun onFailure(e: ApolloException) {
//        Assert.fail(e.message)
//      }
//    })
//    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query)
//        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY).watcher()
//    watcher.enqueueAndWatch(
//        object : Callback<EpisodeHeroNameQuery.Data>() {
//          override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {
//            heroNameList.add(response.data!!.hero!!.name)
//          }
//
//          override fun onFailure(e: ApolloException) {
//            Assert.fail(e.message)
//          }
//        })
//
//    //Another newer call gets updated information
//    server.enqueue(readResource("EpisodeHeroNameResponseNameChange.json"))
//    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
//    watcher.cancel()
//    assertThat(heroNameList[0]).isEqualTo("R2-D2")
//    assertThat(heroNameList[1]).isEqualTo("Artoo")
//    assertThat(heroNameList.size).isEqualTo(2)
//  }
//
//  @Test
//  fun testQueryWatcherNotCalled_WhenCanceled() {
//    val heroNameList: MutableList<String> = ArrayList()
//    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))
//    server.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
//    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
//    watcher.enqueueAndWatch(
//        object : Callback<EpisodeHeroNameQuery.Data>() {
//          override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {
//            heroNameList.add(response.data!!.hero!!.name)
//          }
//
//          override fun onFailure(e: ApolloException) {
//            Assert.fail(e.message)
//          }
//        })
//    watcher.cancel()
//    Utils.enqueueAndAssertResponse(
//        server,
//        "EpisodeHeroNameResponseNameChange.json",
//        apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
//        { response -> !response.hasErrors() }
//    )
//    assertThat(heroNameList[0]).isEqualTo("R2-D2")
//    assertThat(heroNameList.size).isEqualTo(1)
//  }
//
//  @Test
//  fun emptyCacheQueryWatcherCacheOnly() {
//    val watchedHeroes = ArrayList<EpisodeHeroNameQuery.Data.Hero?>()
//    val query = EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))
//    apolloClient.query(query)
//        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
//        .watcher()
//        .enqueueAndWatch(object : Callback<EpisodeHeroNameQuery.Data>() {
//          override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {
//            if (response.data != null) {
//              watchedHeroes.add(response.data!!.hero!!)
//            }
//          }
//
//          override fun onFailure(e: ApolloException) {
//            Assert.fail(e.message)
//          }
//        })
//    server.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
//    apolloClient.query(query).enqueue(object : Callback<EpisodeHeroNameQuery.Data>() {
//      override fun onResponse(response: ApolloResponse<EpisodeHeroNameQuery.Data>) {
//        assertThat(response.data).isNotNull()
//        assertThat(response.data?.hero).isNotNull()
//      }
//
//      override fun onFailure(e: ApolloException) {
//        Assert.fail(e.message)
//      }
//    })
//    assertThat(watchedHeroes).hasSize(1)
//    assertThat(watchedHeroes[0]?.name).isEqualTo("R2-D2")
//  }
//
//  @Test
//  fun queryWatcherWithCacheOnlyNeverGoesToTheNetwork() {
//    runBlocking {
//      val channel = Channel<ApolloResponse<EpisodeHeroNameQuery.Data>>(capacity = Channel.UNLIMITED)
//      val job = launch {
//        apolloClient.query(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)))
//            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
//            .watcher()
//            .refetchResponseFetcher(ApolloResponseFetchers.CACHE_ONLY)
//            .toFlow()
//            .collect {
//              channel.send(it)
//            }
//      }
//
//      val response1 = channel.receive()
//      assertThat(response1.data).isNull()
//      assertThat(response1.isFromCache).isTrue()
//
//      server.enqueue(readResource("StarshipByIdResponse.json"))
//      server.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
//
//      // execute a query that doesn't share any key with the main query
//      // that will trigger a refetch that shouldn't throw
//      apolloClient.query(StarshipByIdQuery("Starship1")).await()
//
//      val response2 = channel.receive()
//      // There should be no data
//      assertThat(response2.data).isNull()
//      assertThat(response2.isFromCache).isTrue()
//
//      job.cancel()
//    }
//  }
//
//  @Test
//  fun queryWatcherWithCacheOnlyCanBeUpdatedFromAnotherQuery() {
//    runBlocking {
//      val channel = Channel<ApolloResponse<EpisodeHeroNameQuery.Data>>(capacity = Channel.UNLIMITED)
//      val job = launch {
//        apolloClient.query(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE)))
//            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
//            .watcher()
//            .refetchResponseFetcher(ApolloResponseFetchers.CACHE_ONLY)
//            .toFlow()
//            .collect {
//              channel.send(it)
//            }
//      }
//
//      val response1 = channel.receive()
//      assertThat(response1.data).isNull()
//      assertThat(response1.isFromCache).isTrue()
//
//      // execute a query that should go to the network and trigger a result from the watcher
//      server.enqueue(readResource("EpisodeHeroNameResponseWithId.json"))
//      apolloClient.query(EpisodeHeroNameQuery(Input.Present(Episode.EMPIRE))).await()
//
//      val response2 = channel.receive()
//
//      assertThat(response2.data?.hero?.name).isEqualTo("R2-D2")
//      assertThat(response2.isFromCache).isTrue()
//
//      job.cancel()
//    }
//  }
}
