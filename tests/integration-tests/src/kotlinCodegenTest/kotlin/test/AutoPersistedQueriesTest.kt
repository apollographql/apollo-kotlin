package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.integration.normalizer.UpdateReviewMutation
import com.apollographql.apollo3.integration.normalizer.type.ColorInput
import com.apollographql.apollo3.integration.normalizer.type.ReviewInput
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

  @Test
  fun mutationsAreSentWithPostRegardlessOfSetting() = runTest(before = { setUp() }, after = { tearDown() }) {
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

    mockServer.enqueue(testFixtureToUtf8("UpdateReviewResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Get, httpMethodForDocumentQueries = HttpMethod.Get)
        .build()

    apolloClient.mutation(UpdateReviewMutation("100", ReviewInput(5, Optional.Absent, ColorInput(Optional.Absent, Optional.Absent, Optional.Absent)))).execute()

    var request = mockServer.takeRequest()
    assertEquals("POST", request.method)
    request = mockServer.takeRequest()
    assertEquals("POST", request.method)
  }

}
