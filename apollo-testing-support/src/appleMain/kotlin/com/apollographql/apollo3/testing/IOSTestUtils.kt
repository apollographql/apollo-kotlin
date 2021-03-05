package com.apollographql.apollo3.testing

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopRun
import platform.CoreFoundation.CFRunLoopStop
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.CoroutineContext

/**
 * A specialized version of `runBlocking` that keeps a CFRunLoop alive so that apple code can dispatch on the main
 * queue. There is more to the story and this might hopefully be merged with runBlocking below
 * but for now that allows us to run integration tests against a mocked server
 */
actual fun <T> runWithMainLoop(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
  var value: T? = null
  GlobalScope.launch(MainLoopDispatcher) {
    value = block()
    CFRunLoopStop(CFRunLoopGetCurrent())
  }
  CFRunLoopRun()

  return value!!
}

actual fun <T> runBlocking(context: CoroutineContext, block: suspend CoroutineScope.() -> T) = kotlinx.coroutines.runBlocking { block() }


@OptIn(InternalCoroutinesApi::class)
private object MainLoopDispatcher : CoroutineDispatcher(), Delay {

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    dispatch_async(dispatch_get_main_queue()) {
      block.run()
    }
  }

  @InternalCoroutinesApi
  override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
      with(continuation) {
        resumeUndispatched(Unit)
      }
    }
  }

  @InternalCoroutinesApi
  override fun invokeOnTimeout(timeMillis: Long, block: Runnable, context: CoroutineContext): DisposableHandle {
    val handle = object : DisposableHandle {
      var disposed = false
        private set

      override fun dispose() {
        disposed = true
      }
    }
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
      if (!handle.disposed) {
        block.run()
      }
    }

    return handle
  }
}

