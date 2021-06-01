package com.apollographql.apollo3.cache.normalized.internal

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSThread
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock
import platform.posix.pthread_tVar
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze


actual class Guard<R: Any> actual constructor(name: String, private val producer: () -> R) {
  private val queue = AtomicReference<List<Callback<R>?>>(emptyList())

  private val pthreadT = nativeHeap.alloc<pthread_tVar>()
  private val mutex = nativeHeap.alloc<pthread_mutex_t>()
  private val cond = nativeHeap.alloc<pthread_cond_t>()

  init {
    pthread_mutex_init(mutex.ptr, null)
    pthread_cond_init(cond.ptr, null)

    val stableRef = StableRef.create(this.freeze())

    pthread_create(pthreadT.ptr, null, staticCFunction { arg ->
      initRuntimeIfNeeded()

      val ref = arg!!.asStableRef<Guard<R>>()

      val guard = ref.get()
      ref.dispose()

      guard.runThread()

      null
    }, stableRef.asCPointer())
  }

  private fun enqueue(callback: Callback<R>?) {
    pthread_mutex_lock(mutex.ptr)
    var items = queue.value
    items += callback
    queue.value = items.freeze()
    pthread_cond_broadcast(cond.ptr)
    pthread_mutex_unlock(mutex.ptr)
  }

  actual fun dispose() {
    enqueue(null)

    pthread_join(pthreadT.value, null)
    nativeHeap.free(pthreadT.rawPtr)
    pthread_mutex_destroy(mutex.ptr)
    pthread_cond_destroy(cond.ptr)
  }

  actual suspend fun <T> access(block: (R) -> T): T {
    val result = suspendCoroutine<Result<T>> { continuation ->
      val continuationPointer = StableRef.create(continuation).asCPointer()

      block.freeze()
      val callback = Callback<R> { s ->
        val result: Result<T> = kotlin.runCatching {
          block(s)
        }

        result.dispatchOnMain(continuationPointer)
      }

      enqueue(callback)
    }

    return result.getOrThrow()
  }

  actual fun writeAndForget(block: (R) -> Unit) {
    val callback = Callback<R> { s ->
      kotlin.runCatching {
        block(s)
      }
    }

    enqueue(callback)
  }


  companion object {
    /**
     * This runs from the background thread
     */
    private fun <S: Any> Guard<S>.runThread() {
      val state = producer()

      while (true) {
        var callback: Callback<S>? = null

        pthread_mutex_lock(mutex.ptr)
        while (true) {
          var items = queue.value
          if (items.isNotEmpty()) {
            callback = items.first()
            if (callback == null) {
              /**
               * We were told to terminate
               */
              return
            }
            items = items.drop(1)
            queue.value = items.freeze()
          }

          if (callback == null) {
            pthread_cond_wait(cond.ptr, mutex.ptr)
          } else {
            break
          }
        }
        pthread_mutex_unlock(mutex.ptr)

        callback?.run?.invoke(state)
      }
    }
  }

  actual fun <T> blockingAccess(block: (R) -> T): T {
    return runBlocking {
      access(block)
    }
  }
}

class Callback<S>(val run: ((S) -> Unit))

private fun <R> Result<R>.dispatchOnMain(continuationPtr: COpaquePointer) {
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
