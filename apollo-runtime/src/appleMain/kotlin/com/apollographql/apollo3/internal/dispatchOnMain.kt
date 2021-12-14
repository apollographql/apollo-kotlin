package com.apollographql.apollo3.internal

import com.apollographql.apollo3.mpp.assertMainThreadOnNative
import kotlinx.cinterop.StableRef
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.native.concurrent.freeze

/**
 * Suspends the current coroutine and calls [block] with a [MainContinuation] that will always dispatch on the main thread
 *
 * [block] will initially be called in the main thread but it's ok to call [MainContinuation.resumeWith] from any thread,
 * [MainContinuation] will ensure that the final result is delivered to the main thread.
 * [block] should not throw. Errors are propagated by passing a [Result] to [MainContinuation.resumeWith]
 *
 * Internally, this works by creating a [StableRef] to the [CancellableContinuation] so that it can be tunneled accross threads
 * without being frozen.
 * Because of this, this only works for coroutines that run in the main thread.
 */
suspend fun <R> suspendAndResumeOnMain(block: (MainContinuation<R>, InvokeOnCancellation) -> Unit): R {
  assertMainThreadOnNative()

  return suspendCancellableCoroutine { continuation ->
    block(MainContinuation(continuation)) { continuation.invokeOnCancellation(it) }
  }
}

typealias InvokeOnCancellation = (CompletionHandler) -> Unit

/**
 * A continuation that will always dispatch on the main thread
 */
class MainContinuation<R>(continuation: CancellableContinuation<R>) {
  init {
    // See https://github.com/apollographql/apollo-android/issues/3347
    // ensureNeverFrozen(continuation)
  }
  private val continuationRef = StableRef.create(continuation)

  fun resumeWith(result: Result<R>) {
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

