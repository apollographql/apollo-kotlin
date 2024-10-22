@file:Suppress("DEPRECATION", "DEPRECATION_ERROR", "unused")

package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.mockserver.MockServer
import com.apollographql.apollo.mockserver.WebsocketMockRequest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMs: Long = 0,
): Unit = TODO()

@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer.enqueueData(
    data: Map<String, Any?>,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMillis: Long = 0,
    statusCode: Int = 200,
): Unit = TODO()

@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer.enqueueData(
    data: Operation.Data,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMillis: Long = 0,
    statusCode: Int = 200,
): Unit = TODO()

/**
 * Extracts the operationId from a graphql-ws message
 */
@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_2)
@ApolloExperimental
suspend fun WebsocketMockRequest.awaitSubscribe(timeout: Duration = 1.seconds, messagesToIgnore: Set<String> = emptySet()): String = TODO()

/**
 * Extracts the operationId from a graphql-ws message, ignores "complete messages"
 */
@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_2)
@ApolloExperimental
suspend fun WebsocketMockRequest.awaitComplete(timeout: Duration = 1.seconds): String = TODO()

@Deprecated(
    "This was only used for internal Apollo tests and is now removed.",
    level = DeprecationLevel.ERROR
)
@ApolloExperimental
fun connectionAckMessage(): String = TODO()

