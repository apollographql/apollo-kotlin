package com.apollographql.apollo.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.apollographql.apollo.benchmark.Utils.dbName
import com.apollographql.apollo.benchmark.Utils.operationBasedQuery
import com.apollographql.apollo.benchmark.Utils.resource
import com.apollographql.apollo.benchmark.test.R
import com.apollographql.cache.normalized.ApolloStore
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.fetchPolicy
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.mockserver.MockRequestBase
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.MockServerHandler
import com.apollographql.apollo.testing.MapTestNetworkTransport
import com.apollographql.apollo.testing.registerTestResponse
import com.apollographql.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.store
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class CacheIncubatingIntegrationTests {
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


