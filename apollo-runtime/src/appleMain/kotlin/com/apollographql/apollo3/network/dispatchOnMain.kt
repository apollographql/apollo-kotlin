package com.apollographql.apollo3.network

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSThread
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.native.concurrent.freeze

/**
 * A trick to support coroutines without relying on `coroutines-mt` It makes the assumption
 * that the Apple coroutines run in the main thread and "tunnels" the continuation through
 * an opaquePointer without freezing so that it can be resumed later on
 *
 * @param continuationPtr a [Continuation] opaque pointer obtained with `StableRef.create(continuation).asCPointer()`
 */
internal fun <R> Result<R>.dispatchOnMain(continuationPtr: COpaquePointer) {
  if (NSThread.isMainThread()) {
    dispatch(continuationPtr)
  } else {
    val continuationWithResultRef = StableRef.create((continuationPtr to this).freeze())

    dispatch_async_f(
        queue = dispatch_get_main_queue(),
        context = continuationWithResultRef.asCPointer(),
        work = staticCFunction { ptr ->
          val continuationWithResultRef2 = ptr!!.asStableRef<Pair<COpaquePointer, Result<R>>>()
          val (continuationPtr2, result) = continuationWithResultRef2.get()
          continuationWithResultRef2.dispose()

          result.dispatch(continuationPtr2)
        }
    )
  }
}

private fun <R> Result<R>.dispatch(continuationPtr: COpaquePointer) {
  val continuationRef = continuationPtr.asStableRef<Continuation<Result<R>>>()
  val continuation = continuationRef.get()
  continuationRef.dispose()

  continuation.resume(this)
}
