package com.apollographql.apollo3.mpp

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.native.concurrent.freeze

class MainContinuation<R>(continuation: CancellableContinuation<R>) {
  private val continuationRef = StableRef.create(continuation)

  fun resume(result: Result<R>) {
    if (NSThread.isMainThread()) {
      resumeInternal(result)
    } else {
      freeze()
      dispatch_async(
          queue = dispatch_get_main_queue(),
          block = {
            initRuntimeIfNeeded()
            resumeInternal(result)
          }.freeze()
      )
    }
  }

  private fun resumeInternal(result: Result<R>) {
    val continuation = continuationRef.get()
    continuationRef.dispose()

    continuation.resumeWith(result)
  }
}

typealias InvokeOnCancellation = (CompletionHandler) -> Unit
suspend fun <R> suspendAndResumeOnMain(block: (MainContinuation<R>, InvokeOnCancellation) -> Unit): R {
  return suspendCancellableCoroutine { continuation ->
    block(MainContinuation(continuation)) { continuation.invokeOnCancellation(it) }
  }
}