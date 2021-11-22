package com.apollographql.apollo3.internal

import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import platform.posix.pthread_create
import platform.posix.pthread_join
import platform.posix.pthread_tVar
import kotlin.native.concurrent.freeze

private class Callback<S>(val run: ((S) -> Unit))

class SingleThreadWorker<S : Any>(private val producer: () -> S, private val finalizer: (S) -> Unit = {}) {
  private val queue = ConcurrentQueue<Callback<S>?>()

  private val pthread = nativeHeap.alloc<pthread_tVar>()

  init {
    val stableRef = StableRef.create(this.freeze())

    pthread_create(pthread.ptr, null, staticCFunction { arg ->
      initRuntimeIfNeeded()

      val ref = arg!!.asStableRef<SingleThreadWorker<S>>()

      val self = ref.get()
      ref.dispose()

      self.runThread()

      null
    }, stableRef.asCPointer())
  }

  private fun enqueue(callback: Callback<S>?) {
    queue.addLast(callback)
  }

  fun dispose() {
    enqueue(null)

    pthread_join(pthread.value, null)
    nativeHeap.free(pthread.rawPtr)

    queue.dispose()
  }

  suspend fun <R> execute(block: (S) -> R) = suspendAndResumeOnMain<R> { mainContinuation, _ ->
    block.freeze()
    val callback = Callback<S> { s ->
      val result: Result<R> = kotlin.runCatching {
        block(s)
      }

      mainContinuation.resumeWith(result)
    }

    enqueue(callback)
  }

  fun executeAndForget(block: (S) -> Unit) {
    val callback = Callback<S> { s ->
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
    private fun <S : Any> SingleThreadWorker<S>.runThread() {
      val state = producer()

      while (true) {
        val callback = queue.removeFirst()
        if (callback == null) {
          // we were told to terminate
          break
        }
        callback.run.invoke(state)
      }

      finalizer(state)
    }
  }
}