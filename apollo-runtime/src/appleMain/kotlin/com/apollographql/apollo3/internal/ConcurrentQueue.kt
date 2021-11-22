package com.apollographql.apollo3.internal

import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.Foundation.NSMutableArray
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock
import kotlin.native.concurrent.freeze

class ConcurrentQueue<T> {
  private val queue = NSMutableArray()
  private val mutex = nativeHeap.alloc<pthread_mutex_t>()
  private val cond = nativeHeap.alloc<pthread_cond_t>()

  init {
    pthread_mutex_init(mutex.ptr, null)
    pthread_cond_init(cond.ptr, null)
  }

  fun addLast(callback: T) {
    pthread_mutex_lock(mutex.ptr)
    queue.addObject(callback.freeze())
    pthread_cond_broadcast(cond.ptr)
    pthread_mutex_unlock(mutex.ptr)
  }

  fun removeFirst(): T {
    pthread_mutex_lock(mutex.ptr)
    while (queue.count.toInt() == 0) {
      pthread_cond_wait(cond.ptr, mutex.ptr)
    }
    @Suppress("UNCHECKED_CAST")
    val item = queue.objectAtIndex(0) as T
    queue.removeObjectAtIndex(0)
    pthread_mutex_unlock(mutex.ptr)

    return item
  }

  fun dispose() {
    pthread_mutex_destroy(mutex.ptr)
    pthread_cond_destroy(cond.ptr)
  }
}