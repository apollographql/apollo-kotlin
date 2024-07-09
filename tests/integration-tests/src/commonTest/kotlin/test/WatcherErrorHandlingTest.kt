package test

import IdCacheKeyGenerator
import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.refetchPolicy
import com.apollographql.apollo.cache.normalized.store
import com.apollographql.apollo.cache.normalized.watch
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.testing.awaitElement
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class WatcherErrorHandlingTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var store: ApolloStore

  private suspend fun setUp() {
    store = ApolloStore(MemoryCacheFactory(), cacheKeyGenerator = IdCacheKeyGenerator)
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).store(store).build()
  }

  private fun tearDown() {
    mockServer.close()
  }

  /**
   * watch() should behave just like toFlow() in the absence of cache writes
   */
  @Test
  fun fetchEmitsAllErrors() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.CacheFirst)
        .watch()
        .test {
          assertIs<CacheMissException>(awaitItem().exception)
          assertIs<ApolloHttpException>(awaitItem().exception)
          cancelAndIgnoreRemainingEvents()
        }

    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.CacheOnly)
        .watch()
        .test {
          assertIs<CacheMissException>(awaitItem().exception)
          cancelAndIgnoreRemainingEvents()
        }

    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .watch()
        .test {
          assertIs<ApolloHttpException>(awaitItem().exception)
          assertIs<CacheMissException>(awaitItem().exception)
          cancelAndIgnoreRemainingEvents()
        }

    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .watch()
        .test {
          assertIs<ApolloHttpException>(awaitItem().exception)
          cancelAndIgnoreRemainingEvents()
        }

    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .watch()
        .test {
          assertIs<CacheMissException>(awaitItem().exception)
          assertIs<ApolloHttpException>(awaitItem().exception)
          cancelAndIgnoreRemainingEvents()
        }
  }

  @Test
  fun refetchEmitsAllErrors() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<ApolloResponse<EpisodeHeroNameQuery.Data>>()

    val query = EpisodeHeroNameQuery(Episode.EMPIRE)

    // The first query should get a "R2-D2" name
    val job = launch {
      mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponseWithId.json"))
      apolloClient.query(query)
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .refetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .collect {
            channel.send(it)
          }
    }
    @Suppress("DEPRECATION")
    assertEquals(channel.awaitElement().data?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    // Due to .refetchPolicy(FetchPolicy.NetworkOnly), a subsequent call will be executed in watch()
    // we enqueue an error so a network exception is emitted
    mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponseNameChange.json"))
    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    @Suppress("DEPRECATION")
    assertIs<ApolloHttpException>(channel.awaitElement().exception)
    job.cancel()
  }

  @Test
  fun fetchEmitsExceptions() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueError(statusCode = 500)
    assertIs<CacheMissException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.CacheFirst)
            .watch()
            .first()
            .exception
    )

    assertIs<CacheMissException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.CacheOnly)
            .watch()
            .first()
            .exception
    )

    mockServer.enqueueError(statusCode = 500)
    assertIs<ApolloHttpException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .watch()
            .first()
            .exception
    )

    mockServer.enqueueError(statusCode = 500)
    assertIs<ApolloHttpException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .watch()
            .first()
            .exception
    )

    mockServer.enqueueError(statusCode = 500)
    assertIs<CacheMissException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.CacheAndNetwork)
            .watch()
            .first()
            .exception
    )
  }

  @Test
  fun refetchEmitsExceptions() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<ApolloResponse<EpisodeHeroNameQuery.Data>>()

    val query = EpisodeHeroNameQuery(Episode.EMPIRE)

    var throwable: Throwable? = null

    // The first query should get a "R2-D2" name
    val job = launch {
      mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponseWithId.json"))
      apolloClient.query(query)
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .refetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .catch { throwable = it }
          .collect {
            channel.send(it)
          }
    }
    @Suppress("DEPRECATION")
    assertEquals(channel.awaitElement().data?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    // Due to .refetchPolicy(FetchPolicy.NetworkOnly), a subsequent call will be executed in watch()
    // we enqueue an error so a network exception is emitted
    mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponseNameChange.json"))
    mockServer.enqueueError(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    @Suppress("DEPRECATION")
    assertIs<ApolloHttpException>(channel.awaitElement().exception)

    assertNull(throwable)

    job.cancel()
  }
}
