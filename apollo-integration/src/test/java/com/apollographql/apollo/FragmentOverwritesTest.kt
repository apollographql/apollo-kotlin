package com.apollographql.apollo

import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.mockResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.integration.fragmentoverwrites.HomeQuery
import com.apollographql.apollo.integration.fragmentoverwrites.fragment.SectionFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FragmentOverwritesTest {

    private lateinit var apolloClient: ApolloClient

    @get:Rule
    val server = MockWebServer()

    private val cacheKeyResolver = object : CacheKeyResolver() {
        override fun fromFieldArguments(field: ResponseField, variables: Operation.Variables): CacheKey {
            return CacheKey.NO_KEY
        }

        override fun fromFieldRecordSet(field: ResponseField, recordSet: Map<String, Any>): CacheKey {
            return (recordSet["id"] as? String)?.let { CacheKey.from(it) } ?: CacheKey.NO_KEY
        }
    }

    @Before
    fun setup() {
        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(Dispatcher(immediateExecutorService()))
            .build()

        apolloClient = ApolloClient.builder()
            .normalizedCache(LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION), cacheKeyResolver)
            .okHttpClient(okHttpClient)
            .dispatcher(immediateExecutor())
            .serverUrl(server.url("/"))
            .build()
    }

    @Test
    fun `doesn't overwrite cache entries when using fragments`() {

        server.enqueue(mockResponse("FragmentOverwritesTestHomeQueryResponse.json"))

        runBlocking {
            val networkResponse = apolloClient.query(HomeQuery()).await()

            assertThat(networkResponse.data?.home?.sectionA?.name).isEqualTo("initialSectionName")
            assertThat(networkResponse.data?.home?.fragments?.sectionFragment?.sectionA?.imageUrl).isEqualTo("initialUrl")

            apolloClient.apolloStore.writeAndPublish(
                HomeQuery(),
                HomeQuery.Data(
                    HomeQuery.Home(
                        sectionA = HomeQuery.SectionA(
                            name = "modifiedSectionName"
                        ),
                        fragments = HomeQuery.Home.Fragments(
                            sectionFragment = SectionFragment(
                                sectionA = SectionFragment.SectionA(
                                    id = "section-id",
                                    imageUrl = "modifiedUrl",
                                ),
                            )
                        )
                    )
                )
            ).execute()

            val cacheResponse = apolloClient.query(HomeQuery())
                .toBuilder()
                .responseFetcher(ApolloResponseFetchers.CACHE_ONLY)
                .build()
                .await()

            assertThat(cacheResponse.data?.home?.sectionA?.name).isEqualTo("modifiedSectionName")
            assertThat(cacheResponse.data?.home?.fragments?.sectionFragment?.sectionA?.imageUrl).isEqualTo("modifiedUrl")
        }
    }
}
