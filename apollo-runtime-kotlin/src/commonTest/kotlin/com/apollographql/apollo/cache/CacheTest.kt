package com.apollographql.apollo.cache

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.interceptor.cache.ApolloCacheInterceptor
import com.apollographql.apollo.mock.MockNetworkTransport
import com.apollographql.apollo.mock.MockQuery
import com.apollographql.apollo.mock.TestLoggerExecutor
import com.apollographql.apollo.runBlocking
import kotlinx.coroutines.flow.single
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("EXPERIMENTAL_API_USAGE")
class CacheTest {
  private lateinit var networkTransport: MockNetworkTransport
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    networkTransport = MockNetworkTransport()
    apolloClient = ApolloClient(
        networkTransport = networkTransport,
        interceptors = listOf(TestLoggerExecutor, ApolloCacheInterceptor())
    )
  }

  @Test
  fun `second request doesn't hit network`() {
    networkTransport.offer("""
      {
        "data": {
          "hero": {
            "__typename": "Hero",
            "name": "Ian Solo"
          }
        }
      }
    """.trimIndent())

    runBlocking {
      var response = apolloClient
          .query(TestQuery())
          .execute()
          .single()

      assertNotNull(response.data)
      assertFalse(response.executionContext[CacheExecutionContext]!!.fromCache)

      response = apolloClient
          .query(TestQuery())
          .execute()
          .single()

      assertNotNull(response.data)
      assertTrue(response.executionContext[CacheExecutionContext]!!.fromCache)
    }
  }
}
