package com.apollographql.apollo3.cache.normalized.sql.internal

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSThread
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.posix.QOS_CLASS_DEFAULT
import platform.posix.open
import platform.posix.pthread_self
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach
import kotlin.native.concurrent.freeze

/**
 * Performs IO operation on background thread by dispatching to global queue.
 * This executor requires caller thread to be main.
 * After operation executed it resumes on the main queue.
 *
 * In order to not freeze the continuation, it passes it around as a [COpaquePointer]
 * so that the coroutine can ultimately resume
 */
actual class IOTaskExecutor actual constructor(name: String){
  actual suspend fun <R> execute(operation: () -> R): R {
    assert(NSThread.isMainThread())
    return suspendCoroutine { continuation ->
      val continuationRef = StableRef.create(continuation)
      dispatchInQueue(
          queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.convert(), 0.convert()),
          continuationPtr = continuationRef.asCPointer(),
          operation = operation,
      )
    }
  }

  actual fun executeAndForget(operation: () -> Unit) {
    execute(queue, Unit) {
      operation()
    }
  }

  companion object {
    private fun <R> dispatchInQueue(queue: dispatch_queue_t, continuationPtr: COpaquePointer, operation: () -> R) {
      execute(queue, continuationPtr to operation) {
        val result = runCatching {
          it.second()
        }
        dispatchInMain(it.first, result.freeze())
      }
    }

    private fun <R> dispatchInMain(continuationPtr: COpaquePointer, result: Result<R>) {
      execute(dispatch_get_main_queue(), continuationPtr to result) {
        val continuationRef = it.first.asStableRef<Continuation<R>>()
        val continuation = continuationRef.get()
        continuationRef.dispose()
        continuation.resumeWith(it.second)
      }
    }

    private inline fun < reified T: Any> execute(queue: dispatch_queue_t, param: T, noinline operation: (T) -> Unit) {
      val ref = StableRef.create((param to operation).freeze())

      dispatch_async_f(
          queue = queue,
          context = ref.asCPointer(),
          work = staticCFunction { ctxPtr ->
            initRuntimeIfNeeded()
            val ctxRef = ctxPtr!!.asStableRef<Pair<T, (T) ->Unit>>()
            val ctxParam = ctxRef.get()
            ctxRef.dispose()

            ctxParam.second.invoke(ctxParam.first)
          }
      )
    }
  }
}

fun currentThreadId(): String {
  return pthread_self()?.rawValue.toString()
}
