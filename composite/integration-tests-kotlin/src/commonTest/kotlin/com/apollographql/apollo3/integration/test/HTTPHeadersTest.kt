package com.apollographql.apollo3.integration.test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.interceptor.cache.withStore
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.flow.single
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class HTTPHeadersTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url())
  }

  @Test
  fun `Test Headers`() {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

    mockServer.enqueue(query, data)

    runWithMainLoop {
      val response = apolloClient.query(query)

      assertNotNull(response.data)

      val recordedRequest = mockServer.takeRequest()
      assertEquals( "POST", recordedRequest.method)
      assertNotEquals( null, recordedRequest.headers["Content-Length"])
      assertNotEquals( "0", recordedRequest.headers["Content-Length"])
    }
  }
}