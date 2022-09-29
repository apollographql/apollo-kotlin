package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Utility method that executes the given [block] with optional [before] and [after] blocks.
 *
 * When [skipDelays] is `true`, the block is executed in [kotlinx.coroutines.test.runTest], otherwise in `runBlocking`.
 */
@ApolloInternal
expect fun runTest(
    skipDelays: Boolean = false,
    context: CoroutineContext = EmptyCoroutineContext,
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
)
