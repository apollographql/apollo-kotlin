package com.apollographql.apollo

import com.apollographql.apollo.Utils.*
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.coroutines.toChannel
import com.apollographql.apollo.coroutines.toDeferred
import com.apollographql.apollo.coroutines.toJob
import com.apollographql.apollo.exception.ApolloParseException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test


class CoroutinesApolloTest {
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
    fun callChannelProducesValue() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).toChannel()
        runBlocking {
            val response = channel.receive()

            assertThat(response.data()!!.hero()!!.name()).isEqualTo("R2-D2")
        }
    }

    @Test
    fun callDeferredProducesValue() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val deferred = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).toDeferred()
        runBlocking {
            assertThat(deferred.await().data()!!.hero()!!.name()).isEqualTo("R2-D2")
        }
    }

    @Test
    fun errorIsTriggered() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("nonsense"))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).toChannel()
        runBlocking {
            var exception: java.lang.Exception? = null
            try {
                channel.receive()
            } catch (e: Exception) {
                exception = e
            }
            assertThat(exception is ApolloParseException).isTrue()
        }
    }

    @Test
    fun prefetchCompletes() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        runBlocking {
            val job = apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                    .toJob()
            job.join()
        }
    }

    @Test
    fun prefetchIsCanceledWhenDisposed() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        runBlocking {
            val job = apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                    .toJob()
            job.cancel()
        }
    }

    @Test
    fun queryWatcherUpdatedSameQueryDifferentResults() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher().toChannel()

        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE))
        apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
                .enqueue(null)

        runBlocking {
            val response0 = channel.receive()
            assertThat(response0.data()!!.hero()!!.name()).isEqualTo("R2-D2")

            val response1 = channel.receive()
            assertThat(response1.data()!!.hero()!!.name()).isEqualTo("Artoo")
        }
    }

    @Test
    fun queryWatcherNotUpdatedSameQuerySameResults() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher().toChannel()

        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
        apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
                .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
                .enqueue(null)

        runBlocking {
            val response0 = channel.receive()
            assertThat(response0.data()!!.hero()!!.name()).isEqualTo("R2-D2")

            assertThat(channel.isEmpty).isEqualTo(true)
        }
    }

    @Test
    fun queryWatcherUpdatedDifferentQueryDifferentResults() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher().toChannel()

        server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"))
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
                .enqueue(null)

        runBlocking {
            val response0 = channel.receive()
            assertThat(response0.data()!!.hero()!!.name()).isEqualTo("R2-D2")

            val response1 = channel.receive()
            assertThat(response1.data()!!.hero()!!.name()).isEqualTo("Artoo")
        }
    }

    @Test
    fun queryWatcherUpdatedConflatedOnlyReturnsLastResult() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher().toChannel(Channel.CONFLATED)

        server.enqueue(mockResponse("HeroAndFriendsNameWithIdsNameChange.json"))
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE)))
                .enqueue(null)

        runBlocking {
            delay(500)
            val response1 = channel.receive()
            assertThat(response1.data()!!.hero()!!.name()).isEqualTo("Artoo")
        }
    }

    @Test
    fun queryWatcherCancelClosesChannel() {
        server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val channel = apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher().toChannel()

        channel.cancel()
        assertThat(channel.isClosedForReceive).isEqualTo(true)
    }


    companion object {

        private val FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json"
        private val FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json"
    }
}
