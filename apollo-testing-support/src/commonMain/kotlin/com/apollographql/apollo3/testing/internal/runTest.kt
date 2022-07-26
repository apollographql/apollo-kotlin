package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Utility method that calls [kotlinx.coroutines.test.runTest], with optional [before] and [after] blocks.
 */
@ApolloInternal
@OptIn(ExperimentalCoroutinesApi::class)
fun runTest(
    context: CoroutineContext = EmptyCoroutineContext,
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
) {
  kotlinx.coroutines.test.runTest(context) {
    before()
    try {
      block()
    } finally {
      after()
    }
  }
}

/**
 * Similar to [runTest] but uses `runBlocking` instead of [kotlinx.coroutines.test.runTest].
 * Use it when delays/timing are important to the test or [kotlinx.coroutines.test.runTest] can't otherwise be used.
 */
@ApolloInternal
expect fun runTestBlocking(
    context: CoroutineContext = EmptyCoroutineContext,
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
)
