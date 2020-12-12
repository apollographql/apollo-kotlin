package com.apollographql.apollo

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.ApolloCall.Callback
import com.apollographql.apollo.ApolloCall.StatusEvent
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth
import io.reactivex.functions.Predicate
import junit.framework.Assert
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mockito
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert.fail
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock

class ApolloWatcherTest {
  private lateinit var apolloClient: ApolloClient

  @get:Rule
  val server = MockWebServer()
  
  @Before
  @Throws(IOException::class)
  fun setUp() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(Utils.immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .dispatcher(Utils.immediateExecutor())
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
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })


    // Another newer call gets updated information
    Utils.enqueueAndAssertResponse(
        server,
        "EpisodeHeroNameResponseNameChange.json",
        apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        { response -> !response.hasErrors() }
    )
    watcher.cancel()
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList[1]).isEqualTo("Artoo")
    Truth.assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(IOException::class, InterruptedException::class, TimeoutException::class, ApolloException::class)
  fun testQueryWatcherUpdated_Store_write() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")

    // Someone writes to the store directly
    val changedKeys: Set<String> = apolloClient.getApolloStore().writeTransaction(object : Transaction<WriteableStore, Set<String>> {
      override fun execute(cache: WriteableStore): Set<String>? {
        val record: Record = Record.builder("2001")
            .addField("name", "Artoo")
            .build()
        return cache.merge(listOf(record), CacheHeaders.NONE)
      }
    })
    apolloClient.getApolloStore().publish(changedKeys)
    Truth.assertThat(heroNameList[1]).isEqualTo("Artoo")
    watcher.cancel()
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherNotUpdated_SameQuery_SameResults() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherUpdated_DifferentQuery_DifferentResults() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    val friendsQuery: HeroAndFriendsNamesWithIDsQuery = HeroAndFriendsNamesWithIDsQuery.builder()
        .episode(Episode.NEWHOPE)
        .build()
    Utils.enqueueAndAssertResponse(
        server,
        "HeroAndFriendsNameWithIdsNameChange.json",
        apolloClient.query(friendsQuery).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        { response -> !response.hasErrors() }
    )
    watcher.cancel()
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList[1]).isEqualTo("Artoo")
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherNotUpdated_DifferentQueries() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    val friendsQuery: HeroAndFriendsNamesWithIDsQuery = HeroAndFriendsNamesWithIDsQuery.builder().episode(Episode.NEWHOPE).build()
    server.enqueue(Utils.mockResponse("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(friendsQuery).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun testRefetchCacheControl() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.refetchResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY) //Force network instead of CACHE_FIRST default
        .enqueueAndWatch(
            object : Callback<EpisodeHeroNameQuery.Data>() {
              override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
                heroNameList.add(response.data()!!.hero()!!.name())
              }

              override fun onFailure(e: ApolloException) {
                Assert.fail(e.message)
              }
            })

    //A different call gets updated information.
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseNameChange.json"))

    //To verify that the updated response comes from server use a different name change
    // -- this is for the refetch
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseNameChangeTwo.json"))
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList[1]).isEqualTo("ArTwo")
    Truth.assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherUpdated_SameQuery_DifferentResults_cacheOnly() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(query).enqueue(object : Callback<EpisodeHeroNameQuery.Data>() {
      override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {}
      override fun onFailure(e: ApolloException) {
        Assert.fail(e.message)
      }
    })
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query)
        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })

    //Another newer call gets updated information
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseNameChange.json"))
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList[1]).isEqualTo("Artoo")
    Truth.assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(Exception::class)
  fun testQueryWatcherNotCalled_WhenCanceled() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data()!!.hero()!!.name())
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    watcher.cancel()
    Utils.enqueueAndAssertResponse(
        server,
        "EpisodeHeroNameResponseNameChange.json",
        apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY),
        { response -> !response.hasErrors() }
    )
    Truth.assertThat(heroNameList[0]).isEqualTo("R2-D2")
    Truth.assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  @Throws(Exception::class)
  fun emptyCacheQueryWatcherCacheOnly() {
    val watchedHeroes: MutableList<EpisodeHeroNameQuery.Hero?> = ArrayList<EpisodeHeroNameQuery.Hero?>()
    val query = EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))
    apolloClient.query(query)
        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
        .watcher()
        .enqueueAndWatch(object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            if (response.data() != null) {
              watchedHeroes.add(response.data()!!.hero()!!)
            }
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(query).enqueue(object : Callback<EpisodeHeroNameQuery.Data>() {
      override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
        assertThat(response.data()).isNotNull()
        assertThat(response.data()?.hero()).isNotNull()
      }

      override fun onFailure(e: ApolloException) {
        Assert.fail(e.message)
      }
    })
    Truth.assertThat(watchedHeroes).hasSize(1)
    assertThat(watchedHeroes[0]?.name()).isEqualTo("R2-D2")
  }

  @Test
  @Throws(Exception::class)
  fun queryWatcher_onStatusEvent_properlyCalled() {
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    val callback = Mockito.mock(ApolloCall.Callback::class.java) as ApolloCall.Callback<EpisodeHeroNameQuery.Data>
    watcher.enqueueAndWatch(callback)
    val inOrder: InOrder = inOrder(callback)
    inOrder.verify(callback).onStatusEvent(StatusEvent.SCHEDULED)
    inOrder.verify(callback).onStatusEvent(StatusEvent.FETCH_CACHE)
    inOrder.verify(callback).onStatusEvent(StatusEvent.FETCH_NETWORK)
    inOrder.verify(callback).onStatusEvent(StatusEvent.COMPLETED)
  }
}