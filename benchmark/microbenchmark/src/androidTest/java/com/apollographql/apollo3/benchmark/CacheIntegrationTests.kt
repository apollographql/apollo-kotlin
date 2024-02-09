package com.apollographql.apollo3.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.apollographql.apollo3.benchmark.Utils.dbName
import com.apollographql.apollo3.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo3.benchmark.Utils.resource
import com.apollographql.apollo3.benchmark.test.R
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.mockserver.MockRequestBase
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.MockServerHandler
import com.apollographql.apollo3.testing.MapTestNetworkTransport
import com.apollographql.apollo3.testing.registerTestResponse
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class CacheIntegrationTests {
  @get:Rule
  val benchmarkRule = BenchmarkRule()

  @Test
  fun concurrentQueriesTestNetworkTransportMemory() {
    concurrentQueries(MemoryCacheFactory(), withMockServer = false)
  }

  @Test
  fun concurrentQueriesTestNetworkTransportSql() {
    Utils.dbFile.delete()
    val cacheFactory = SqlNormalizedCacheFactory(dbName)
    concurrentQueries(cacheFactory, withMockServer = false)
  }

  @Test
  fun concurrentQueriesTestNetworkTransportMemoryThenSql() {
    Utils.dbFile.delete()
    val cacheFactory = MemoryCacheFactory().chain(SqlNormalizedCacheFactory(dbName))
    concurrentQueries(cacheFactory, withMockServer = false)
  }


  private fun concurrentQueries(cacheFactory: NormalizedCacheFactory, withMockServer: Boolean) {
    val mockServer = MockServer.Builder()
        .handler(
            object : MockServerHandler {
              private val mockResponse = MockResponse.Builder()
                  .statusCode(200)
                  .body(resource(R.raw.calendar_response_simple).readByteString())
                  .build()

              override fun handle(request: MockRequestBase): MockResponse {
                return mockResponse
              }
            }
        )
        .build()

    val client = ApolloClient.Builder()
        .let {
          if (withMockServer) {
            it.serverUrl(runBlocking { mockServer.url() })
          } else {
            it.networkTransport(MapTestNetworkTransport())
          }
        }
        .store(createApolloStore(cacheFactory))
        .build()
    if (!withMockServer) {
      client.registerTestResponse(operationBasedQuery, operationBasedQuery.parseJsonResponse(resource(R.raw.calendar_response_simple).jsonReader()).data!!)
    }

    benchmarkRule.measureRepeated {
      runBlocking {
        (1..CONCURRENCY).map {
          launch {
            // Let each job execute a few queries
            repeat(WORK_LOAD) {
              client.query(operationBasedQuery).fetchPolicy(FetchPolicy.NetworkOnly).execute().dataOrThrow()
              client.query(operationBasedQuery).fetchPolicy(FetchPolicy.CacheOnly).execute().dataOrThrow()
            }
          }
        }
            // Wait for all jobs to finish
            .joinAll()
      }
    }
  }

  private fun createApolloStore(cacheFactory: NormalizedCacheFactory): ApolloStore {
    return ApolloStore(cacheFactory)
  }


  companion object {
    private const val CONCURRENCY = 10
    private const val WORK_LOAD = 8
  }
}


