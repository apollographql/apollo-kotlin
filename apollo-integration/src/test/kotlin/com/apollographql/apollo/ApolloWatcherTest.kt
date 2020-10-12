package com.apollographql.apollo

import com.apollographql.apollo.Utils.enqueueAndAssertResponse
import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.Record.Companion.builder
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import junit.framework.Assert
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException

class ApolloWatcherTest {
  private lateinit var apolloClient: ApolloClient

  val server = MockWebServer()

  @Before
  @Throws(IOException::class)
  fun setUp() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .dispatcher(immediateExecutor())
        .okHttpClient(okHttpClient)
        .logger(object : Logger {
          override fun log(priority: Int, message: String, t: Throwable?, vararg args: Any) {
            println(String.format(message, *args))
            t?.printStackTrace()
          }
        })
        .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherUpdated_SameQuery_DifferentResults() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })


    // Another newer call gets updated information
    enqueueAndAssertResponse(
        server,
        "EpisodeHeroNameResponseNameChange.json",
        apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        Predicate<Response<EpisodeHeroNameQuery.Data?>> { response -> !response.hasErrors() }
    )
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("Artoo")
    assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(IOException::class, InterruptedException::class, TimeoutException::class, ApolloException::class)
  fun testQueryWatcherUpdated_Store_write() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    assertThat(heroNameList[0]).isEqualTo("R2-D2")

    // Someone writes to the store directly
    val changedKeys = apolloClient!!.apolloStore.writeTransaction(object : Transaction<WriteableStore, Set<String>> {
      override fun execute(cache: WriteableStore): Set<String>? {
        val record = builder("2001")
            .addField("name", "Artoo")
            .build()
        return cache.merge(listOf(record), CacheHeaders.NONE)
      }
    })
    apolloClient!!.apolloStore.publish(changedKeys)
    assertThat(heroNameList[1]).isEqualTo("Artoo")
    watcher.cancel()
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherNotUpdated_SameQuery_SameResults() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherUpdated_DifferentQuery_DifferentResults() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    val friendsQuery = HeroAndFriendsNamesWithIDsQuery(episode = Input.fromNullable(Episode.NEWHOPE))
    enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsNameChange.json",
        apolloClient!!.query(friendsQuery).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        Predicate<Response<HeroAndFriendsNamesWithIDsQuery.Data?>> { response -> !response.hasErrors() }
    )
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("Artoo")
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherNotUpdated_DifferentQueries() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    val friendsQuery: HeroAndFriendsNamesWithIDsQuery = HeroAndFriendsNamesWithIDsQuery(episode = Input.fromNullable(Episode.NEWHOPE))
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient!!.query(friendsQuery).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun testRefetchCacheControl() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.refetchResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY) //Force network instead of CACHE_FIRST default
        .enqueueAndWatch(
            object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
              override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
                heroNameList.add(response.data!!.hero!!.name)
              }

              override fun onFailure(e: ApolloException) {
                Assert.fail(e.cause!!.message)
              }
            })

    //A different call gets updated information.
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"))

    //To verify that the updated response comes from server use a different name change
    // -- this is for the refetch
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChangeTwo.json"))
    apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("ArTwo")
    assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherUpdated_SameQuery_DifferentResults_cacheOnly() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient!!.query(query).enqueue(object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {}
      override fun onFailure(e: ApolloException) {
        Assert.fail(e.message)
      }
    })
    val watcher = apolloClient!!.query(query)
        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })

    //Another newer call gets updated information
    server.enqueue(mockResponse("EpisodeHeroNameResponseNameChange.json"))
    apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("Artoo")
    assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherNotCalled_WhenCanceled() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient!!.query(query).watcher()
    watcher.enqueueAndWatch(
        object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    watcher.cancel()
    enqueueAndAssertResponse(
        server,
        "EpisodeHeroNameResponseNameChange.json",
        apolloClient!!.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        Predicate<Response<EpisodeHeroNameQuery.Data?>> { response -> !response.hasErrors() }
    )
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun emptyCacheQueryWatcherCacheOnly() {
    val watchedHeroes: MutableList<EpisodeHeroNameQuery.Hero?> = ArrayList()
    val query = EpisodeHeroNameQuery(fromNullable(Episode.EMPIRE))
    apolloClient!!.query(query)
        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
        .watcher()
        .enqueueAndWatch(object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            if (response.data != null) {
              watchedHeroes.add(response.data!!?.hero)
            }
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient!!.query(query).enqueue(object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
        assertThat(response.data).isNotNull()
        assertThat(response.data!!?.hero).isNotNull()
      }

      override fun onFailure(e: ApolloException) {
        Assert.fail(e.message)
      }
    })
    assertThat(watchedHeroes).hasSize(1)
    assertThat(watchedHeroes[0]!!?.name).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun queryWatcher_onStatusEvent_properlyCalled() {
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(episode = Input.fromNullable(Episode.EMPIRE))
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient!!.query(query).watcher()
    val callback: ApolloCall.Callback<EpisodeHeroNameQuery.Data> = Mockito.mock(ApolloCall.Callback::class.java) as ApolloCall.Callback<EpisodeHeroNameQuery.Data>
    watcher.enqueueAndWatch(callback)
    val inOrder = Mockito.inOrder(callback)
    inOrder.verify(callback).onStatusEvent(ApolloCall.StatusEvent.SCHEDULED)
    inOrder.verify(callback).onStatusEvent(ApolloCall.StatusEvent.FETCH_CACHE)
    inOrder.verify(callback).onStatusEvent(ApolloCall.StatusEvent.FETCH_NETWORK)
    inOrder.verify(callback).onStatusEvent(ApolloCall.StatusEvent.COMPLETED)
  }
}