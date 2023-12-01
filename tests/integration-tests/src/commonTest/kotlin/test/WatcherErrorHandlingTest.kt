package test

import IdCacheKeyGenerator
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.refetchPolicy
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.receiveOrTimeout
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    mockServer.close()
    apolloClient.close()
  }

  @Test
  fun fetchIgnoreAllErrors() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()
    val jobs = mutableListOf<Job>()

    jobs += launch {
      mockServer.enqueueString(statusCode = 500)
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
      mockServer.enqueueString(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.NetworkFirst)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    jobs += launch {
      mockServer.enqueueString(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    jobs += launch {
      mockServer.enqueueString(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.CacheAndNetwork)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }

    // Consume all errors and assume the channel is empty
    repeat(8) {
      channel.receive()
    }
    channel.assertEmpty()
    jobs.forEach { it.cancel() }
  }

  @Test
  fun refetchIgnoreAllErrors() = runTest(before = { setUp() }, after = { tearDown() }) {
    val channel = Channel<EpisodeHeroNameQuery.Data?>()

    val query = EpisodeHeroNameQuery(Episode.EMPIRE)

    // The first query should get a "R2-D2" name
    val job = launch {
      mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponseWithId.json"))
      apolloClient.query(query)
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .refetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .collect {
            channel.send(it.data)
          }
    }
    assertEquals(channel.receiveOrTimeout()?.hero?.name, "R2-D2")

    // Another newer call gets updated information with "Artoo"
    // Due to .refetchPolicy(FetchPolicy.NetworkOnly), a subsequent call will be executed in watch()
    // we enqueue an error so a network exception is thrown
    mockServer.enqueueString(testFixtureToUtf8("EpisodeHeroNameResponseNameChange.json"))
    mockServer.enqueueString(statusCode = 500)
    apolloClient.query(query).fetchPolicy(FetchPolicy.NetworkOnly)
        .execute()
        .data
        ?.hero
        ?.name
        .apply {
          assertEquals("Artoo", this)
        }

    channel.receive().apply {
      assertNull(this)
    }
    channel.assertEmpty()
    job.cancel()
  }

  private suspend fun <T> Flow<T>.awaitList(count: Int, timeoutMillis: Long = 500): List<T>  = withTimeout(timeoutMillis){
    take(count).toList()
  }

  @Test
  fun fetchThrows() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.CacheFirst)
        .watch()
        .awaitList(2)
        .map { it.exception }
        .apply {
          assertIs<CacheMissException>(get(0))
          assertIs<ApolloHttpException>(get(1))
        }


      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.CacheOnly)
          .watch()
          .first()
          .exception
          .apply {
            assertIs<CacheMissException>(this)
          }

    mockServer.enqueueString(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .watch()
        .awaitList(2)
        .map { it.exception }
        .apply {
          assertIs<ApolloHttpException>(get(0))
          assertIs<CacheMissException>(get(1))
        }

    mockServer.enqueueString(statusCode = 500)
      apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
          .fetchPolicy(FetchPolicy.NetworkOnly)
          .watch()
          .first()
          .exception
          .apply {
            assertIs<ApolloHttpException>(this)
          }

    mockServer.enqueueString(statusCode = 500)
    apolloClient.query(EpisodeHeroNameQuery(Episode.EMPIRE))
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .watch()
        .awaitList(2)
        .map { it.exception }
        .apply {
          assertIs<CacheMissException>(get(0))
          assertIs<ApolloHttpException>(get(1))
        }
  }
}
