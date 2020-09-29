package com.apollographql.apollo

import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.type.Episode.EMPIRE
import com.apollographql.apollo.integration.normalizer.type.Episode.NEWHOPE
import com.apollographql.apollo.rx3.Rx3Apollo
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.functions.Predicate
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class Rx3ApolloTest {
  private lateinit var apolloClient: ApolloClient

  @get:Rule
  val server = MockWebServer()

  @Before
  fun setup() {
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

  @Test
  @Throws(Exception::class)
  fun callProducesValue() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .test()
        .assertNoErrors()
        .assertComplete()
        .assertValue({ response ->
          assertThat(response.data()?.hero()?.name()).isEqualTo("R2-D2")
          true
        })
  }

  @Test
  @Throws(Exception::class)
  fun callIsCanceledWhenDisposed() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val testObserver = TestObserver<Response<EpisodeHeroNameQuery.Data>>()
    val disposable: Disposable = Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .subscribeWith(testObserver)
    disposable.dispose()
    testObserver.assertComplete()
    Truth.assertThat(testObserver.isDisposed()).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun prefetchCompletes() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    Rx3Apollo
        .from(apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .test()
        .assertNoErrors()
        .assertComplete()
  }

  @Test
  @Throws(Exception::class)
  fun prefetchIsCanceledWhenDisposed() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val testObserver: TestObserver<EpisodeHeroNameQuery.Data> = TestObserver<EpisodeHeroNameQuery.Data>()
    val disposable: Disposable = Rx3Apollo
        .from(apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))))
        .observeOn(TestScheduler())
        .subscribeWith(testObserver)
    disposable.dispose()
    testObserver.assertNotComplete()
    Truth.assertThat(testObserver.isDisposed()).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun queryWatcherUpdatedSameQueryDifferentResults() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val observer: TestObserver<EpisodeHeroNameQuery.Data> = TestObserver<EpisodeHeroNameQuery.Data>()
    Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map({ response -> response.data() })
        .subscribeWith(observer)
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE))
    apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE)))
        .responseFetcher(NETWORK_ONLY)
        .enqueue(null)
    observer.assertValueCount(2)
        .assertValueAt(0) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("R2-D2")
          true
        }
        .assertValueAt(1) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("Artoo")
          true
        }
  }

  @Test
  @Throws(Exception::class)
  fun retryDoesNotThrow() {
    server.enqueue(MockResponse().setResponseCode(500))
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val observer: TestObserver<EpisodeHeroNameQuery.Data> = TestObserver<EpisodeHeroNameQuery.Data>()
    Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .retry(1)
        .map({ response -> response.data() })
        .subscribeWith(observer)
    observer.assertValueCount(1)
        .assertValueAt(0) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("R2-D2")
          true
        }
  }

  @Test
  @Throws(Exception::class)
  fun queryWatcherNotUpdatedSameQuerySameResults() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val observer: TestObserver<EpisodeHeroNameQuery.Data> = TestObserver<EpisodeHeroNameQuery.Data>()
    Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map({ response -> response.data() })
        .subscribeWith(observer)
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
        .enqueue(null)
    observer
        .assertValueCount(1)
        .assertValueAt(0) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("R2-D2")
          true
        }
  }

  @Test
  @Throws(Exception::class)
  fun queryWatcherUpdatedDifferentQueryDifferentResults() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val observer: TestObserver<EpisodeHeroNameQuery.Data> = TestObserver<EpisodeHeroNameQuery.Data>()
    Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map({ response -> response.data() })
        .subscribeWith(observer)
    server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"))
    apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(NEWHOPE))).enqueue(null)
    observer
        .assertValueCount(2)
        .assertValueAt(0) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("R2-D2")
          true
        }
        .assertValueAt(1) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("Artoo")
          true
        }
  }

  @Test
  @Throws(Exception::class)
  fun queryWatcherNotCalledWhenCanceled() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val testObserver: TestObserver<EpisodeHeroNameQuery.Data> = TestObserver<EpisodeHeroNameQuery.Data>()
    val scheduler = TestScheduler()
    val disposable: Disposable = Rx3Apollo
        .from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).watcher())
        .map({ response -> response.data() })
        .observeOn(scheduler)
        .subscribeWith(testObserver)
    scheduler.triggerActions()
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE))
    apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE))).responseFetcher(NETWORK_ONLY)
        .enqueue(null)
    disposable.dispose()
    scheduler.triggerActions()
    testObserver
        .assertValueCount(1)
        .assertValueAt(0) { data ->
          assertThat(data?.hero()?.name()).isEqualTo("R2-D2")
          true
        }
  }

  @Test
  fun disposingAnOperationDoesNotThrowUndeliverableException() {
    /*
     * A simple cache that will always throw errors
     */
    val cacheFactory = object: NormalizedCacheFactory<NormalizedCache>() {
      override fun create(recordFieldAdapter: RecordFieldJsonAdapter): NormalizedCache {
        return object: NormalizedCache() {
          override fun clearAll() {
            throw Exception("not implemented")
          }

          override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
            throw Exception("not implemented")
          }

          override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
            throw Exception("not implemented")
          }

          override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
            throw Exception("not implemented")
          }
        }
      }
    }

    val apolloClient = ApolloClient.builder()
        .normalizedCache(cacheFactory)
        .serverUrl("https://unused/")
        .build()


    val savedHandler = RxJavaPlugins.getErrorHandler()

    var undeliverableException: Throwable? = null
    RxJavaPlugins.setErrorHandler {
      undeliverableException = it
    }

    val operation = apolloClient.apolloStore.write(EpisodeHeroNameQuery(Input.fromNullable(EMPIRE)), EpisodeHeroNameQuery.Data(EpisodeHeroNameQuery.Hero("", "")))
    val testObserver = Rx3Apollo.from(operation)
        .test()

    testObserver.dispose()

    // Since there is no cancellation mechanism for the ApolloStoreOperation, the only way to see if an error is thrown is to wait here
    Thread.sleep(200)
    Truth.assertThat(undeliverableException == null).isTrue()
    RxJavaPlugins.setErrorHandler(savedHandler)
  }

  companion object {
    private const val FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json"
    private const val FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json"
  }
}