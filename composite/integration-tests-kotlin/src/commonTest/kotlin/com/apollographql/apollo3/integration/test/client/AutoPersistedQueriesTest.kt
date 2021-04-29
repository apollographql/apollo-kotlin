package com.apollographql.apollo3.integration.test.client

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.readResource
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.runWithMainLoop
import com.apollographql.apollo3.withAutoPersistedQueries
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoPersistedQueriesTest {
  private lateinit var mockServer: MockServer

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()

  }

  @Test
  fun withApqsDoesntSendDocument() = runWithMainLoop {
    mockServer.enqueue(readResource("HeroNameResponse.json"))

    val apolloClient = ApolloClient(mockServer.url()).withAutoPersistedQueries()

    apolloClient.query(HeroNameQuery())

    val request = mockServer.takeRequest()

    assertFalse(request.body.utf8().contains("query"))
  }

  @Test
  fun withApqsRetriesAfterError() = runWithMainLoop {
    mockServer.enqueue("""
      {
        "errors": [
          {
            "message": "PersistedQueryNotFound"
          }
        ]
      }
    """.trimIndent()
    )

    mockServer.enqueue(readResource("HeroNameResponse.json"))

    val apolloClient = ApolloClient(mockServer.url()).withAutoPersistedQueries()

    apolloClient.query(HeroNameQuery())

    var request = mockServer.takeRequest()
    assertFalse(request.body.utf8().contains("query"))
    request = mockServer.takeRequest()
    assertTrue(request.body.utf8().contains("query"))
  }
}