package com.apollographql.apollo3.testing.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopRun
import platform.CoreFoundation.CFRunLoopStop
import kotlin.coroutines.CoroutineContext

@ApolloInternal
actual typealias ApolloTestResult = Unit

@ApolloInternal
@OptIn(DelicateCoroutinesApi::class)
actual fun runTest(
    skipDelays: Boolean,
    context: CoroutineContext,
    before: suspend CoroutineScope.() -> Unit,
    after: suspend CoroutineScope.() -> Unit,
    block: suspend CoroutineScope.() -> Unit,
) {
  var throwable: Throwable? = null
  GlobalScope.launch(context + Dispatchers.Main) {
    try {
      before()
      try {
        block()
      } finally {
        after()
      }
    } catch (t: Throwable) {
      throwable = t
    } finally {
      CFRunLoopStop(CFRunLoopGetCurrent())
    }
  }
  CFRunLoopRun()
  throwable?.let { throw it }
}
