package com.apollographql.apollo3.internal

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import platform.Foundation.NSThread
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.CoroutineContext

actual fun defaultDispatcher(requested: CoroutineDispatcher?): CoroutineDispatcher {
  check(requested == null) {
    "Changing the dispatcher is not supported on Apple targets"
  }
  check(NSThread.isMainThread) {
    "defaultDispatcher mush be called from the main thread"
  }
  
  return DefaultDispatcher
}


actual class BackgroundDispatcher actual constructor() {
  init {
    check(NSThread.isMainThread) {
      "WebSocketDispatcher must be called from the main thread"
    }
  }
  actual val coroutineDispatcher: CoroutineDispatcher
    get() = DefaultDispatcher

  actual fun dispose() {
  }
}

@OptIn(InternalCoroutinesApi::class)
private object DefaultDispatcher: CoroutineDispatcher(), Delay {

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    dispatch_async(dispatch_get_main_queue()) {
      block.run()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, timeMillis * 1_000_000), dispatch_get_main_queue()) {
      with(continuation) {
        resumeUndispatched(Unit)
      }
    }
  }

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

actual class DefaultMutex actual constructor(): Mutex {
  override fun <T> lock(block: () -> T): T {
    return block()
  }

}