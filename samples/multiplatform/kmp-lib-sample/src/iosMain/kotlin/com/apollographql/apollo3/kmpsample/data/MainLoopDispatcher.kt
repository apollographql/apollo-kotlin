package com.apollographql.apollo3.kmpsample.data

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.CoroutineContext

@InternalCoroutinesApi
object MainLoopDispatcher : CoroutineDispatcher(), Delay {

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
