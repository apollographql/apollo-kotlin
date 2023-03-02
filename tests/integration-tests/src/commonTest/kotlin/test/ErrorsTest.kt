package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertNull

class ErrorsTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun ignorePartialDataOnClient() = runTest(before = { setUp() }, after = { tearDown() }) {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .ignorePartialData(true)
        .build()
    mockServer.enqueue("""
      {
        "data": {
          "hero": {
            "name": "R2-D2"
          }
        },
        "errors": [
          {
            "message": "Something went wrong"
          }
        ]
      }
    """.trimIndent())

    val response = apolloClient.query(HeroNameQuery()).execute()
    assertNull(response.data)
  }

  @Test
  fun ignorePartialDataOnCall() = runTest(before = { setUp() }, after = { tearDown() }) {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .build()
    mockServer.enqueue("""
      {
        "data": {
          "hero": {
            "name": "R2-D2"
          }
        },
        "errors": [
          {
            "message": "Something went wrong"
          }
        ]
      }
    """.trimIndent())

    val response = apolloClient.query(HeroNameQuery()).ignorePartialData(true).execute()
    assertNull(response.data)
  }

}
