@file:OptIn(ApolloExperimental::class)

package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseResponse
import com.apollographql.apollo.benchmark.Utils.getIncubatingMemoryThenSqlCacheFactory
import com.apollographql.apollo.benchmark.Utils.getIncubatingSqlCacheFactory
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.apollo.pagination.UsersPageQuery
import com.apollographql.apollo.pagination.cache.Cache
import com.apollographql.apollo.pagination.cache.Cache.cache
import com.apollographql.apollo.testing.MapTestNetworkTransport
import com.apollographql.apollo.testing.registerTestResponse
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.api.CacheKey
import com.apollographql.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.cache.normalized.apolloStore
import com.apollographql.cache.normalized.fetchPolicy
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.normalizedCache
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration

class CacheIncubatingPaginationTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun paginationMemory() {
    paginationWritesAndRead(MemoryCacheFactory(), paginationEnabled = true)
  }

  @Test
  fun paginationSql() {
    paginationWritesAndRead(getIncubatingSqlCacheFactory(), paginationEnabled = true)
  }

  @Test
  fun paginationMemoryThenSql() {
    paginationWritesAndRead(getIncubatingMemoryThenSqlCacheFactory(), paginationEnabled = true)
  }

  @Test
  fun noPaginationMemory() {
    paginationWritesAndRead(MemoryCacheFactory(), paginationEnabled = false)
  }

  @Test
  fun noPaginationSql() {
    paginationWritesAndRead(getIncubatingSqlCacheFactory(), paginationEnabled = false)
  }

  @Test
  fun noPaginationMemoryThenSql() {
    paginationWritesAndRead(getIncubatingMemoryThenSqlCacheFactory(), paginationEnabled = false)
  }

  private fun paginationWritesAndRead(cacheFactory: NormalizedCacheFactory, paginationEnabled: Boolean) {
    val client = ApolloClient.Builder().networkTransport(MapTestNetworkTransport())
        .run {
          if (paginationEnabled) {
            cache(cacheFactory)
          } else {
            normalizedCache(
                cacheFactory,
                typePolicies = Cache.typePolicies,
                fieldPolicies = Cache.fieldPolicies,
                connectionTypes = emptySet(),
                embeddedFields = Cache.embeddedFields,
                maxAges = Cache.maxAges,
                defaultMaxAge = Duration.INFINITE,
                keyScope = CacheKey.Scope.TYPE,
                enableOptimisticUpdates = false,
                writeToCacheAsynchronously = false,
            )
          }
        }
        .build()
    val paginationPage1Query = UsersPageQuery(first = Optional.present(10))
    val paginationPage2Query = UsersPageQuery(first = Optional.present(10), after = Optional.present("cursor_10"))

    benchmarkRule.measureRepeated {
      runWithMeasurementDisabled {
        runBlocking {
          client.apolloStore.clearAll()
        }
        client.registerTestResponse(paginationPage1Query, paginationPage1Query.parseResponse(resource(R.raw.pagination_page_1).jsonReader())
            .dataOrThrow()
        )
        client.registerTestResponse(paginationPage2Query, paginationPage2Query.parseResponse(resource(R.raw.pagination_page_2).jsonReader())
            .dataOrThrow()
        )
      }

      val combinedData =
        runBlocking {
          // Write page 1
          client.query(paginationPage1Query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

          // Write page 2
          client.query(paginationPage2Query).fetchPolicy(FetchPolicy.NetworkOnly).execute()

          // Read combined pages from cache
          client.query(paginationPage1Query).fetchPolicy(FetchPolicy.CacheOnly).execute().dataOrThrow()
        }

      runWithMeasurementDisabled {
        if (paginationEnabled) {
          check(combinedData.users!!.edges.size == 20)
        } else {
          check(combinedData.users!!.edges.size == 10)
        }
      }
    }
  }
}
