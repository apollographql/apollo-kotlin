package com.apollographql.apollo.testing.internal

import com.apollographql.apollo.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withContext

/**
 * Utility method that executes the given [block] with optional [before] and [after] blocks and disables
 * the skipDelay behaviour from the coroutines `runTest`. Our tests use delay() in some situations.
 */
@ApolloInternal
fun runTest(
    block: suspend CoroutineScope.() -> Unit,
) = runTest(before = {}, after = {}, block)

/**
 * We should probably deprecate this overload and remove the before/after state and lateinit
 * variables in various tests.There are > 150 instances of it though, so I'm not pushing the
 * deprecation button just yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ApolloInternal
fun runTest(
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): TestResult {
  return kotlinx.coroutines.test.runTest {
    /**
     * See https://github.com/Kotlin/kotlinx.coroutines/issues/3179
     */
    withContext(Dispatchers.Default.limitedParallelism(1)) {
      before()
      block()
      after()
    }
  }
}
