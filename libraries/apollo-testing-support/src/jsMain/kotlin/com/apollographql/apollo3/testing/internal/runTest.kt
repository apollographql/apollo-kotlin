package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

// https://youtrack.jetbrains.com/issue/KT-60561
class PromiseOfUnit(executor: (resolve: (Unit) -> Unit, reject: (Throwable) -> Unit) -> Unit) : Promise<Unit>(executor = executor)

@ApolloInternal
actual typealias ApolloTestResult = PromiseOfUnit

@ApolloInternal
@OptIn(DelicateCoroutinesApi::class)
actual fun runTest(
    skipDelays: Boolean,
    context: CoroutineContext,
    before: suspend CoroutineScope.() -> Unit,
    after: suspend CoroutineScope.() -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): ApolloTestResult {
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
  }.unsafeCast<ApolloTestResult>()
}
