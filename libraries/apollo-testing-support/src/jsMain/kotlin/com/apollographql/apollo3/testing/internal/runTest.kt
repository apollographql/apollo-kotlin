package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

// https://youtrack.jetbrains.com/issue/KT-21846/
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE", "INCOMPATIBLE_MATCHING")
@ApolloInternal
actual typealias ApolloTestResult = Promise<Any>

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
  }
}
