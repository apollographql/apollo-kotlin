package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.autoPersistedQueries
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import readResource
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

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Post).build()

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

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Post).build()

    apolloClient.query(HeroNameQuery())

    var request = mockServer.takeRequest()
    assertFalse(request.body.utf8().contains("query"))
    request = mockServer.takeRequest()
    assertTrue(request.body.utf8().contains("query"))
  }
}