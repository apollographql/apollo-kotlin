package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

@ApolloInternal
actual typealias ApolloTestResult = JsPromiseInterfaceForTesting

// https://youtrack.jetbrains.com/issue/KT-60561
@ApolloInternal
@JsName("Promise")
external class JsPromiseInterfaceForTesting {
  fun then(onFulfilled: ((JsAny) -> Unit), onRejected: ((JsAny) -> Unit)): JsPromiseInterfaceForTesting
  fun then(onFulfilled: ((JsAny) -> Unit)): JsPromiseInterfaceForTesting
}

@ApolloInternal
@OptIn(DelicateCoroutinesApi::class)
actual fun runTest(
    skipDelays: Boolean,
    context: CoroutineContext,
    before: suspend CoroutineScope.() -> Unit,
    after: suspend CoroutineScope.() -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): ApolloTestResult {
  @Suppress("INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION")
  return if (skipDelays) {
    kotlinx.coroutines.test.runTest(context) {
      before()
      try {
        block()
      } finally {
        after()
      }
    }
  } else {
    GlobalScope.promise(context = context) {
      before()
      try {
        block()
      } finally {
        after()
      }
    }
  }.unsafeCast()
}
