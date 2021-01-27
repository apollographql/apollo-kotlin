package com.apollographql.apollo.cache.normalized.sql.internal

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSThread
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.QOS_CLASS_DEFAULT
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach
import kotlin.native.concurrent.freeze

/**
 * Performs IO operation on background thread by dispatching to global queue.
 * This executor requires caller thread to be main.
 * After operation executed it resumes on the main queue.
 */
internal actual class IOTaskExecutor {
  actual suspend fun <R> execute(operation: () -> R): R {
    return suspendCoroutine { continuation ->
      execute(
          continuation = continuation,
          operation = operation,
      )
    }
  }

  companion object {
    private fun <R> execute(continuation: Continuation<R>, operation: () -> R) {
      assert(NSThread.isMainThread())
      val continuationPtr = StableRef.create(continuation).asCPointer()
      dispatch_async_f(
          queue = dispatch_get_global_queue(QOS_CLASS_DEFAULT.convert(), 0.convert()),
          context = DetachedObjectGraph { (continuationPtr to { operation() }) }.asCPointer(),
          work = staticCFunction { ctxPtr ->
            initRuntimeIfNeeded()
            autoreleasepool {
              val (continuation, execute) = DetachedObjectGraph<Pair<COpaquePointer, () -> R>>(ctxPtr).attach()
              val result = kotlin.runCatching { execute() }
              result.dispatchOnMain(continuation)
            }
          }
      )
    }

    private fun <R> Result<R>.dispatchOnMain(continuationPtr: COpaquePointer) {
      val continuationWithResultRef = StableRef.create((continuationPtr to this).freeze())
      dispatch_async_f(
          queue = dispatch_get_main_queue(),
          context = continuationWithResultRef.asCPointer(),
          work = staticCFunction { ptr ->
            val continuationWithResultRef = ptr!!.asStableRef<Pair<COpaquePointer, Result<R>>>()
            val (continuationPtr, result) = continuationWithResultRef.get()
            continuationWithResultRef.dispose()
            result.resumeContinuation(continuationPtr)
          }
      )
    }

    private fun <R> Result<R>.resumeContinuation(continuationPtr: COpaquePointer) {
      val continuationRef = continuationPtr.asStableRef<Continuation<R>>()
      val continuation = continuationRef.get()
      continuationRef.dispose()
      continuation.resumeWith(this)
    }
  }
}
