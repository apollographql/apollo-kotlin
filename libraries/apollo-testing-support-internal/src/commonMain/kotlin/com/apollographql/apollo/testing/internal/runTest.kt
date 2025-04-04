package com.apollographql.apollo.testing.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.withContext


fun runTest(
    block: suspend CoroutineScope.() -> Unit,
) = runTest({}, {}, block)

fun runTest(
    before: suspend CoroutineScope.() -> Unit = {},
    after: suspend CoroutineScope.() -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): TestResult {
  return kotlinx.coroutines.test.runTest {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
      before()
      block()
      after()
    }
  }
}