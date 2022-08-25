package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

@ApolloInternal
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
actual fun runTest(
    skipDelays: Boolean,
    context: CoroutineContext,
    before: suspend CoroutineScope.() -> Unit,
    after: suspend CoroutineScope.() -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): dynamic {
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
  }
}
