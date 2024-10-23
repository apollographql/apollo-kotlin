@file:Suppress("DEPRECATION", "DEPRECATION_ERROR", "unused")

package com.apollographql.apollo.testing

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.mockserver.MockServer
import kotlinx.coroutines.CoroutineScope

@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@ApolloExperimental
class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

/**
 * A convenience function that makes sure the MockServer and ApolloClient are properly closed at the end of the test
 */
@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@ApolloExperimental
fun mockServerTest(
    clientBuilder: ApolloClient.Builder.() -> Unit = {},
    block: suspend MockServerTest.() -> Unit
): Unit = TODO()