package com.apollographql.apollo.cache.normalized.sql.internal

import com.apollographql.apollo.cache.normalized.sql.ApolloDatabase
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSThread
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

/**
 * Performs database operation on background thread via [Worker] API.
 * This executor requires caller thread to be main.
 * After database operation execution it resumes on main thread.
 */
internal actual class DatabaseRequestExecutor actual constructor(
    private val database: ApolloDatabase
) {
  private val worker: Worker = Worker.start()

  actual suspend fun <R> execute(operation: ApolloDatabase.() -> R): R {
    return suspendCoroutine { continuation ->
      Request(
          worker = worker,
          database = database,
          operation = operation,
          continuation = continuation,
      ).execute()
    }
  }

  private class Request<R>(
      val worker: Worker,
      val database: ApolloDatabase,
      val operation: ApolloDatabase.() -> R,
      val continuation: Continuation<R>
  ) {
    fun execute() {
      assert(NSThread.isMainThread())
      val continuationPtr = StableRef.create(continuation).asCPointer()
      worker.execute(
          mode = TransferMode.SAFE,
          producer = { (continuationPtr to { operation(database) }).freeze() },
          job = {
            autoreleasepool {
              initRuntimeIfNeeded()
              val execute = it.second
              val result = kotlin.runCatching { execute() }
              result.dispatchOnMain(continuationPtr)
            }
          }
      )
    }

    private fun Result<R>.dispatchOnMain(continuationPtr: COpaquePointer) {
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

    private fun Result<R>.resumeContinuation(continuationPtr: COpaquePointer) {
      val continuationRef = continuationPtr.asStableRef<Continuation<R>>()
      val continuation = continuationRef.get()
      continuationRef.dispose()
      continuation.resumeWith(this)
    }
  }
}
