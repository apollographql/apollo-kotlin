package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Utility method that executes the given [block] with optional [before] and [after] blocks.
 *
 * When [skipDelays] is `true`, the block is executed in [kotlinx.coroutines.test.runTest], otherwise in `runBlocking`.
 */
@ApolloInternal
@OptIn(ExperimentalCoroutinesApi::class)
fun runTest(
    skipDelays: Boolean = false,
    context: CoroutineContext = EmptyCoroutineContext,
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
) {
  if (skipDelays) {
    kotlinx.coroutines.test.runTest(context) {
      before()
      try {
        block()
      } finally {
        after()
      }
    }
  } else {
    runBlockingOrPromise(context) {
      before()
      try {
        block()
      } finally {
        after()
      }
    }
  }
}

internal expect fun runBlockingOrPromise(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit,
)
