package com.apollographql.apollo3

import com.apollographql.apollo3.ApolloCall.Callback
import com.apollographql.apollo3.Utils.receiveOrTimeout
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.Record
import com.apollographql.apollo3.cache.normalized.internal.Transaction
import com.apollographql.apollo3.cache.normalized.internal.WriteableStore
import com.apollographql.apollo3.coroutines.await
import com.apollographql.apollo3.coroutines.toFlow
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameWithIdQuery
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo3.integration.normalizer.StarshipByIdQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.cache.normalized.internal.RealApolloStore
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import junit.framework.Assert
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.TimeoutException

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
        .normalizedCache(MemoryCacheFactory(maxSizeBytes = Int.MAX_VALUE), IdFieldCacheKeyResolver())
        .build()
  }

  @Test
  fun testQueryWatcherUpdated_SameQuery_DifferentResults() {
    val heroNameList: MutableList<String> = ArrayList()
    val query = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
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
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("Artoo")
    assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  @Throws(IOException::class, InterruptedException::class, TimeoutException::class, ApolloException::class)
  fun testQueryWatcherUpdated_Store_write() = runBlocking {
    val channel = Channel<EpisodeHeroNameWithIdQuery.Data?>()

    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val job = launch {
      apolloClient.query(EpisodeHeroNameWithIdQuery(Input.present(Episode.EMPIRE)))
          .watcher()
          .toFlow()
          .collect {
            channel.send(it.data)
          }
    }

    assertThat(channel.receiveOrTimeout()?.hero?.name).isEqualTo("R2-D2")

    // Someone writes to the store directly
    val changedKeys: Set<String> = (apolloClient.apolloStore as RealApolloStore).writeTransaction {
        val record: Record = Record(
            key = "2001",
            fields = mapOf("name" to "Artoo")
        )
        it.merge(listOf(record), CacheHeaders.NONE)
    }

    apolloClient.apolloStore.publish(changedKeys)
    assertThat(channel.receiveOrTimeout()?.hero?.name).isEqualTo("Artoo")

    job.cancel()
  }

  @Test
  fun testQueryWatcherNotUpdated_SameQuery_SameResults() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  fun testQueryWatcherUpdated_DifferentQuery_DifferentResults() = runBlocking {
    val channel = Channel<EpisodeHeroNameWithIdQuery.Data?>(capacity = Channel.UNLIMITED)

    // id: 2001, name = R2-D2
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))

    val job = async {
      apolloClient.query(EpisodeHeroNameWithIdQuery(Input.present(Episode.EMPIRE))).watcher().toFlow().collect {
        channel.send(it.data)
      }
    }

    assertThat(channel.receiveOrTimeout()?.hero?.name).isEqualTo("R2-D2")

    server.enqueue(Utils.mockResponse("HeroAndFriendsNameWithIdsNameChange.json"))
    apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE)))
        .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .await()
        .let {
          assertThat(it.errors).isNull()
        }

    assertThat(channel.receiveOrTimeout()?.hero?.name).isEqualTo("Artoo")

    job.cancel()
  }

  @Test
  fun testQueryWatcherNotUpdated_DifferentQueries() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    val friendsQuery: HeroAndFriendsNamesWithIDsQuery = HeroAndFriendsNamesWithIDsQuery(Input.present(Episode.NEWHOPE))
    server.enqueue(Utils.mockResponse("HeroAndFriendsNameWithIdsResponse.json"))
    apolloClient.query(friendsQuery).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  fun testRefetchCacheControl() {
    val heroNameList: MutableList<String> = ArrayList()
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val query = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.refetchResponseFetcher(ApolloResponseFetchers.NETWORK_ONLY) //Force network instead of CACHE_FIRST default
        .enqueueAndWatch(
            object : Callback<EpisodeHeroNameQuery.Data>() {
              override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
                heroNameList.add(response.data!!.hero!!.name)
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
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("ArTwo")
    assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  fun testQueryWatcherUpdated_SameQuery_DifferentResults_cacheOnly() {
    val heroNameList: MutableList<String> = ArrayList()
    val query = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
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
            heroNameList.add(response.data!!.hero!!.name)
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })

    //Another newer call gets updated information
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseNameChange.json"))
    apolloClient.query(query).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY).enqueue(null)
    watcher.cancel()
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList[1]).isEqualTo("Artoo")
    assertThat(heroNameList.size).isEqualTo(2)
  }

  @Test
  fun testQueryWatcherNotCalled_WhenCanceled() {
    val heroNameList: MutableList<String> = ArrayList()
    val query: EpisodeHeroNameQuery = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    val watcher: ApolloQueryWatcher<EpisodeHeroNameQuery.Data> = apolloClient.query(query).watcher()
    watcher.enqueueAndWatch(
        object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            heroNameList.add(response.data!!.hero!!.name)
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
    assertThat(heroNameList[0]).isEqualTo("R2-D2")
    assertThat(heroNameList.size).isEqualTo(1)
  }

  @Test
  fun emptyCacheQueryWatcherCacheOnly() {
    val watchedHeroes = ArrayList<EpisodeHeroNameQuery.Data.Hero?>()
    val query = EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))
    apolloClient.query(query)
        .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
        .watcher()
        .enqueueAndWatch(object : Callback<EpisodeHeroNameQuery.Data>() {
          override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
            if (response.data != null) {
              watchedHeroes.add(response.data!!.hero!!)
            }
          }

          override fun onFailure(e: ApolloException) {
            Assert.fail(e.message)
          }
        })
    server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
    apolloClient.query(query).enqueue(object : Callback<EpisodeHeroNameQuery.Data>() {
      override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {
        assertThat(response.data).isNotNull()
        assertThat(response.data?.hero).isNotNull()
      }

      override fun onFailure(e: ApolloException) {
        Assert.fail(e.message)
      }
    })
    assertThat(watchedHeroes).hasSize(1)
    assertThat(watchedHeroes[0]?.name).isEqualTo("R2-D2")
  }

  @Test
  fun queryWatcherWithCacheOnlyNeverGoesToTheNetwork() {
    runBlocking {
      val channel = Channel<Response<EpisodeHeroNameQuery.Data>>(capacity = Channel.UNLIMITED)
      val job = launch {
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
            .watcher()
            .refetchResponseFetcher(ApolloResponseFetchers.CACHE_ONLY)
            .toFlow()
            .collect {
              channel.send(it)
            }
      }

      val response1 = channel.receive()
      assertThat(response1.data).isNull()
      assertThat(response1.isFromCache).isTrue()

      server.enqueue(Utils.mockResponse("StarshipByIdResponse.json"))
      server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))

      // execute a query that doesn't share any key with the main query
      // that will trigger a refetch that shouldn't throw
      apolloClient.query(StarshipByIdQuery("Starship1")).await()

      val response2 = channel.receive()
      // There should be no data
      assertThat(response2.data).isNull()
      assertThat(response2.isFromCache).isTrue()

      job.cancel()
    }
  }

  @Test
  fun queryWatcherWithCacheOnlyCanBeUpdatedFromAnotherQuery() {
    runBlocking {
      val channel = Channel<Response<EpisodeHeroNameQuery.Data>>(capacity = Channel.UNLIMITED)
      val job = launch {
        apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE)))
            .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
            .watcher()
            .refetchResponseFetcher(ApolloResponseFetchers.CACHE_ONLY)
            .toFlow()
            .collect {
              channel.send(it)
            }
      }

      val response1 = channel.receive()
      assertThat(response1.data).isNull()
      assertThat(response1.isFromCache).isTrue()

      // execute a query that should go to the network and trigger a result from the watcher
      server.enqueue(Utils.mockResponse("EpisodeHeroNameResponseWithId.json"))
      apolloClient.query(EpisodeHeroNameQuery(Input.present(Episode.EMPIRE))).await()

      val response2 = channel.receive()

      assertThat(response2.data?.hero?.name).isEqualTo("R2-D2")
      assertThat(response2.isFromCache).isTrue()

      job.cancel()
    }
  }
}
