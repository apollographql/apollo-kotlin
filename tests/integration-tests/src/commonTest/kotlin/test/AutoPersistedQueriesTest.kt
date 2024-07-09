package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.apollo.integration.normalizer.UpdateReviewMutation
import com.apollographql.apollo.integration.normalizer.type.ColorInput
import com.apollographql.apollo.integration.normalizer.type.ReviewInput
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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
    mockServer.close()
  }

  @Test
  fun withApqsDoesntSendDocument() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Post)
        .build()

    apolloClient.query(HeroNameQuery()).execute()

    val request = mockServer.awaitRequest()

    assertFalse(request.body.utf8().contains("query"))
  }

  @Test
  fun canDisableApqsPerQuery() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).autoPersistedQueries().build()

    apolloClient.query(HeroNameQuery())
        .enableAutoPersistedQueries(false)
        .execute()

    val request = mockServer.awaitRequest()

    assertTrue(request.method.lowercase() == "post")
    assertTrue(request.body.utf8().contains("query"))
  }

  @Test
  fun withApqsRetriesAfterError() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString("""
      {
        "errors": [
          {
            "message": "PersistedQueryNotFound"
          }
        ]
      }
    """.trimIndent()
    )

    mockServer.enqueueString(testFixtureToUtf8("HeroNameResponse.json"))

    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).autoPersistedQueries().build()

    apolloClient.query(HeroNameQuery()).execute()

    var request = mockServer.awaitRequest()
    assertFalse(request.body.utf8().contains("query"))
    request = mockServer.awaitRequest()
    assertTrue(request.body.utf8().contains("query"))
  }

  @Test
  fun mutationsAreSentWithPostRegardlessOfSetting() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString("""
      {
        "errors": [
          {
            "message": "PersistedQueryNotFound"
          }
        ]
      }
    """.trimIndent()
    )

    mockServer.enqueueString(testFixtureToUtf8("UpdateReviewResponse.json"))

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Get, httpMethodForDocumentQueries = HttpMethod.Get)
        .build()

    apolloClient.mutation(UpdateReviewMutation("100", ReviewInput(5, Optional.Absent, ColorInput(Optional.Absent, Optional.Absent, Optional.Absent)))).execute()

    var request = mockServer.awaitRequest()
    assertEquals("POST", request.method)
    request = mockServer.awaitRequest()
    assertEquals("POST", request.method)
  }

}
