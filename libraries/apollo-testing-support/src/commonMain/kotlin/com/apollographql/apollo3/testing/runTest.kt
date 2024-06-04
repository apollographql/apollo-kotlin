package com.apollographql.apollo3.testing

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.CoroutineScope
import okio.use

@ApolloExperimental
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

/**
 * A convenience function that makes sure the MockServer and ApolloClient are properly closed at the end of the test
 */
@ApolloExperimental
@Deprecated("This is only used for internal Apollo tests and will be removed in a future version.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Suppress("DEPRECATION")
fun mockServerTest(
    skipDelays: Boolean = true,
    clientBuilder: ApolloClient.Builder.() -> Unit = {},
    block: suspend MockServerTest.() -> Unit
) = runTest(skipDelays) {
  MockServer().use { mockServer ->
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .apply(clientBuilder)
        .build()
        .use {apolloClient ->
          MockServerTest(mockServer, apolloClient, this).block()
        }
  }
}
