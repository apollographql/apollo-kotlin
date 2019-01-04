package com.apollographql.apollo

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.apollographql.apollo.Utils.*
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.fetcher.ApolloResponseFetchers.NETWORK_ONLY
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesWithIDsQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.livedata.LiveDataApollo
import com.apollographql.apollo.livedata.LiveDataResponse
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException

@RunWith(JUnit4::class)
class ApolloLiveDataTest {
    private lateinit var apolloClient: ApolloClient
    private lateinit var mockWebServer: MockWebServer

    private val FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json"
    private val FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json"
    private val FILE_HERO_AND_FRIEND_NAMD_CHANGE = "HeroAndFriendsNameWithIdsNameChange.json"

    @Rule
    @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    @Throws(IOException::class)
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(Dispatcher(immediateExecutorService()))
            .build()

        apolloClient = ApolloClient.builder()
            .serverUrl(mockWebServer.url("/"))
            .dispatcher(immediateExecutor())
            .okHttpClient(okHttpClient)
            .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), IdFieldCacheKeyResolver())
            .build()
    }

    @After
    @Throws(IOException::class)
    fun stopServer() {
        mockWebServer.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun callProducesValue() {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val observer = mock<Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>>>()
        val liveData = LiveDataApollo.from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))))
        liveData.observeForever(observer)

        assertNotNull(liveData.value)
        verify(observer).onChanged(liveData.value)
        assertThat(liveData.value, instanceOf(LiveDataResponse.Success::class.java))

        val success = liveData.value as LiveDataResponse.Success
        assertThat(success.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success.data?.hero()?.name(), `is`("R2-D2"))
    }

    @Test
    @Throws(Exception::class)
    fun prefetchCompletes() {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val observer = mock<Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>>>()
        val liveData = LiveDataApollo.from<EpisodeHeroNameQuery.Data>(apolloClient.prefetch(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))))
        liveData.observeForever(observer)

        assertNotNull(liveData.value)
        verify(observer).onChanged(liveData.value)
        assertThat(liveData.value, instanceOf(LiveDataResponse.Complete::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun queryWatcherUpdatedSameQueryDifferentResults() {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val observer = mock<Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>>>()
        val liveData = LiveDataApollo.from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher())
        liveData.observeForever(observer)

        assertNotNull(liveData.value)
        verify(observer).onChanged(liveData.value)
        assertThat(liveData.value, instanceOf(LiveDataResponse.Success::class.java))

        val success = liveData.value as LiveDataResponse.Success
        assertThat(success.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success.data?.hero()?.name(), `is`("R2-D2"))

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE))
        apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
            .responseFetcher(NETWORK_ONLY)
            .enqueue(null)

        val success2 = liveData.value as LiveDataResponse.Success
        assertThat(success2.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success2.data?.hero()?.name(), `is`("Artoo"))
    }

    @Test
    @Throws(Exception::class)
    fun queryWatcherNotUpdatedSameQuerySameResults() {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val observer = mock<Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>>>()
        val liveData = LiveDataApollo.from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher())
        liveData.observeForever(observer)

        assertNotNull(liveData.value)
        verify(observer).onChanged(liveData.value)
        assertThat(liveData.value, instanceOf(LiveDataResponse.Success::class.java))

        val success = liveData.value as LiveDataResponse.Success
        assertThat(success.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success.data?.hero()?.name(), `is`("R2-D2"))

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
        apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
            .responseFetcher(NETWORK_ONLY)
            .enqueue(null)

        verifyNoMoreInteractions(observer)

        val success2 = liveData.value as LiveDataResponse.Success
        assertThat(success2.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success2.data?.hero()?.name(), `is`("R2-D2"))
    }

    @Test
    @Throws(Exception::class)
    fun queryWatcherUpdatedDifferentQueryDifferentResults() {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val observer = mock<Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>>>()
        val liveData = LiveDataApollo.from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher())
        liveData.observeForever(observer)

        assertNotNull(liveData.value)
        verify(observer).onChanged(liveData.value)
        assertThat(liveData.value, instanceOf(LiveDataResponse.Success::class.java))

        val success = liveData.value as LiveDataResponse.Success
        assertThat(success.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success.data?.hero()?.name(), `is`("R2-D2"))

        mockWebServer.enqueue(mockResponse(FILE_HERO_AND_FRIEND_NAMD_CHANGE))
        apolloClient.query(HeroAndFriendsNamesWithIDsQuery(Input.fromNullable(Episode.NEWHOPE))).enqueue(null)

        val success2 = liveData.value as LiveDataResponse.Success
        assertThat(success2.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success2.data?.hero()?.name(), `is`("Artoo"))
    }

    @Test
    @Throws(Exception::class)
    fun queryWatcherNotCalledWhenCanceled() {
        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))

        val observer = mock<Observer<LiveDataResponse<EpisodeHeroNameQuery.Data>>>()
        val liveData = LiveDataApollo.from(apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE))).watcher())
        liveData.observeForever(observer)

        assertNotNull(liveData.value)
        verify(observer).onChanged(liveData.value)
        assertThat(liveData.value, instanceOf(LiveDataResponse.Success::class.java))

        val success = liveData.value as LiveDataResponse.Success
        assertThat(success.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success.data?.hero()?.name(), `is`("R2-D2"))

        liveData.removeObserver(observer)

        mockWebServer.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
        apolloClient.query(EpisodeHeroNameQuery(Input.fromNullable(Episode.EMPIRE)))
            .responseFetcher(NETWORK_ONLY)
            .enqueue(null)

        verifyNoMoreInteractions(observer)

        val success2 = liveData.value as LiveDataResponse.Success
        assertThat(success2.data?.hero()?.__typename(), `is`("Droid"))
        assertThat(success2.data?.hero()?.name(), `is`("R2-D2"))
    }
}