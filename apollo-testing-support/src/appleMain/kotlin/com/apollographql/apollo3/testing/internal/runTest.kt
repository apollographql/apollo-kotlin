package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

@ApolloInternal
@OptIn(ExperimentalCoroutinesApi::class)
actual fun runTest(
    skipDelays: Boolean,
    context: CoroutineContext,
    before: suspend CoroutineScope.() -> Unit,
    after: suspend CoroutineScope.() -> Unit,
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
    runBlocking(context) {
      before()
      try {
        block()
      } finally {
        after()
      }
    }
  }
}
