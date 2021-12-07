package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class AutoPersistedQueriesTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun withApqsDoesntSendDocument() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Post)
        .build()

    apolloClient.query(HeroNameQuery()).execute()

    val request = mockServer.takeRequest()

    assertFalse(request.body.utf8().contains("query"))
  }

  @Test
  fun canDisableApqsPerQuery() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).autoPersistedQueries().build()

    apolloClient.query(HeroNameQuery())
        .enableAutoPersistedQueries(false)
        .execute()

    val request = mockServer.takeRequest()

    assertTrue(request.method.lowercase() == "post")
    assertTrue(request.body.utf8().contains("query"))
  }

  @Test
  fun withApqsRetriesAfterError() = runTest(before = { setUp() }, after = { tearDown() }) {
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

    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).autoPersistedQueries().build()

    apolloClient.query(HeroNameQuery()).execute()

    var request = mockServer.takeRequest()
    assertFalse(request.body.utf8().contains("query"))
    request = mockServer.takeRequest()
    assertTrue(request.body.utf8().contains("query"))
  }
}
