package test

import IdCacheKeyGenerator
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.refetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.receiveOrTimeout
import kotlinx.coroutines.Job
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

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun fetchEmitsAllErrors() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()
    val jobs = mutableListOf<Job>()

    jobs += launch {
      mockServer.enqueue(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.CacheFirst)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    jobs += launch {
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.CacheOnly)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    jobs += launch {
      mockServer.enqueue(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.NetworkFirst)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    jobs += launch {
      mockServer.enqueue(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    jobs += launch {
      mockServer.enqueue(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    channel.assertCount(8)
    jobs.forEach { it.cancel() }
  }

  @Test
  fun refetchEmitsAllErrors() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<ApolloResponse<EpisodeHeroNameQuery.Data>>()

    val query = EpisodeHeroNameQuery(Episode.EMPIRE)

    // The first query should get a "R2-D2" name
    val job = launch {
      mockServer.enqueue(testFixtureToUtf8("EpisodeHeroNameResponseWithId.json"))
      apolloClient.query(query)
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .refetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .collect {
            channel.send(it)
          }
    }
    assertEquals(channel.receiveOrTimeout().data?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    // Due to .refetchPolicy(FetchPolicy.NetworkOnly), a subsequent call will be executed in watch()
    // we enqueue an error so a network exception is emitted
    mockServer.enqueue(testFixtureToUtf8("EpisodeHeroNameResponseNameChange.json"))
    mockServer.enqueue(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    assertIs<ApolloHttpException>(channel.receiveOrTimeout().exception)
    job.cancel()
  }

  @Test
  fun fetchEmitsExceptions() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(statusCode = 500)
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

    mockServer.enqueue(statusCode = 500)
    assertIs<ApolloHttpException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .watch()
            .first()
            .exception
    )

    mockServer.enqueue(statusCode = 500)
    assertIs<ApolloHttpException>(
        apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .watch()
            .first()
            .exception
    )

    mockServer.enqueue(statusCode = 500)
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
      mockServer.enqueue(testFixtureToUtf8("EpisodeHeroNameResponseWithId.json"))
      apolloClient.query(query)
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .refetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .catch { throwable = it }
          .collect {
            channel.send(it)
          }
    }
    assertEquals(channel.receiveOrTimeout().data?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    // Due to .refetchPolicy(FetchPolicy.NetworkOnly), a subsequent call will be executed in watch()
    // we enqueue an error so a network exception is emitted
    mockServer.enqueue(testFixtureToUtf8("EpisodeHeroNameResponseNameChange.json"))
    mockServer.enqueue(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

    assertIs<ApolloHttpException>(channel.receiveOrTimeout().exception)

    assertNull(throwable)

    job.cancel()
  }
}
