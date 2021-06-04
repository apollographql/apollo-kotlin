package com.apollographql.apollo3.mpp

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.native.concurrent.freeze

internal class MainContinuation<R>(private val continuationPtr: COpaquePointer) {
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

  private fun <R> resumeInternal(result: Result<R>) {
    val continuationRef = continuationPtr.asStableRef<Continuation<Result<R>>>()
    val continuation = continuationRef.get()
    continuationRef.dispose()

    continuation.resume(result)
  }
}

internal suspend fun <R> suspendAndResumeOnMain(block: (MainContinuation<R>) -> Unit): R {
  val result = suspendCancellableCoroutine<Result<R>> { continuation ->
    val continuationPtr = StableRef.create(continuation).asCPointer()

    block(MainContinuation(continuationPtr))
  }

  return result.getOrThrow()
}