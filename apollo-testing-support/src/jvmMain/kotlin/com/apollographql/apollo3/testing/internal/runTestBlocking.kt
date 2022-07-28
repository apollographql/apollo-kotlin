package com.apollographql.apollo3.testing.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

internal actual fun runTestBlocking(
    context: CoroutineContext,
    before: suspend CoroutineScope.() -> Unit,
    after: suspend CoroutineScope.() -> Unit,
    block: suspend CoroutineScope.() -> Unit,
) = runBlocking(context) {
  before()
  try {
    block()
  } finally {
    after()
  }
}
