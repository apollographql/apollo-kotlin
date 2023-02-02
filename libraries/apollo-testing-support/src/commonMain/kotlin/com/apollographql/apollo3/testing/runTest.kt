package com.apollographql.apollo3.testing

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.mockserver.MockServer
import kotlinx.coroutines.CoroutineScope
import okio.use

@ApolloExperimental
class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

/**
 * A convenience function that makes sure the MockServer and ApolloClient are properly closed at the end of the test
 */
@ApolloExperimental
fun mockServerTest(block: suspend MockServerTest.() -> Unit) = com.apollographql.apollo3.testing.internal.runTest(true) {
  val mockServer = MockServer()

  val apolloClient = ApolloClient.Builder()
      .serverUrl(mockServer.url())
      .build()

  try {
    apolloClient.use {
      MockServerTest(mockServer, it, this).block()
    }
  } finally {
    mockServer.stop()
  }
}
