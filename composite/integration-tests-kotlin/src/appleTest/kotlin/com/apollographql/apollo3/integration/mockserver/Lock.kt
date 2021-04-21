package com.apollographql.apollo3.integration.mockserver

/**
 * From org.jetbrains.kotlinx:atomicfu:0.15.1
 */
import platform.posix.*
import interop.*
import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.native.internal.NativePtr
import com.apollographql.apollo3.integration.mockserver.SynchronizedObject.Status.*

class SynchronizedObject {

  val lock = AtomicReference(LockState(UNLOCKED, 0, 0))

  public fun lock() {
    val currentThreadId = pthread_self()!!
    while (true) {
      val state = lock.value
      when (state.status) {
        UNLOCKED -> {
          val thinLock = LockState(THIN, 1, 0, currentThreadId)
          if (lock.compareAndSet(state, thinLock))
            return
        }
        THIN -> {
          if (currentThreadId == state.ownerThreadId) {
            // reentrant lock
            val thinNested = LockState(THIN, state.nestedLocks + 1, state.waiters, currentThreadId)
            if (lock.compareAndSet(state, thinNested))
              return
          } else {
            // another thread is trying to take this lock -> allocate native mutex
            val mutex = mutexPool.allocate()
            mutex.lock()
            val fatLock = LockState(FAT, state.nestedLocks, state.waiters + 1, state.ownerThreadId, mutex)
            if (lock.compareAndSet(state, fatLock)) {
              //block the current thread waiting for the owner thread to release the permit
              mutex.lock()
              tryLockAfterResume(currentThreadId)
              return
            } else {
              // return permit taken for the owner thread and release mutex back to the pool
              mutex.unlock()
              mutexPool.release(mutex)
            }
          }
        }
        FAT -> {
          if (currentThreadId == state.ownerThreadId) {
            // reentrant lock
            val nestedFatLock = LockState(FAT, state.nestedLocks + 1, state.waiters, state.ownerThreadId, state.mutex)
            if (lock.compareAndSet(state, nestedFatLock)) return
          } else if (state.ownerThreadId != null) {
            val fatLock = LockState(FAT, state.nestedLocks, state.waiters + 1, state.ownerThreadId, state.mutex)
            if (lock.compareAndSet(state, fatLock)) {
              fatLock.mutex!!.lock()
              tryLockAfterResume(currentThreadId)
              return
            }
          }
        }
      }
    }
  }

  public fun tryLock(): Boolean {
    val currentThreadId = pthread_self()!!
    while (true) {
      val state = lock.value
      if (state.status == UNLOCKED) {
        val thinLock = LockState(THIN, 1, 0, currentThreadId)
        if (lock.compareAndSet(state, thinLock))
          return true
      } else {
        if (currentThreadId == state.ownerThreadId) {
          val nestedLock = LockState(state.status, state.nestedLocks + 1, state.waiters, currentThreadId, state.mutex)
          if (lock.compareAndSet(state, nestedLock))
            return true
        } else {
          return false
        }
      }
    }
  }

  public fun unlock() {
    val currentThreadId = pthread_self()!!
    while (true) {
      val state = lock.value
      require(currentThreadId == state.ownerThreadId) { "Thin lock may be only released by the owner thread, expected: ${state.ownerThreadId}, real: $currentThreadId" }
      when (state.status) {
        THIN -> {
          // nested unlock
          if (state.nestedLocks == 1) {
            val unlocked = LockState(UNLOCKED, 0, 0)
            if (lock.compareAndSet(state, unlocked))
              return
          } else {
            val releasedNestedLock =
                LockState(THIN, state.nestedLocks - 1, state.waiters, state.ownerThreadId)
            if (lock.compareAndSet(state, releasedNestedLock))
              return
          }
        }
        FAT -> {
          if (state.nestedLocks == 1) {
            // last nested unlock -> release completely, resume some waiter
            val releasedLock = LockState(FAT, 0, state.waiters - 1, null, state.mutex)
            if (lock.compareAndSet(state, releasedLock)) {
              releasedLock.mutex!!.unlock()
              return
            }
          } else {
            // lock is still owned by the current thread
            val releasedLock =
                LockState(FAT, state.nestedLocks - 1, state.waiters, state.ownerThreadId, state.mutex)
            if (lock.compareAndSet(state, releasedLock))
              return
          }
        }
        else -> error("It is not possible to unlock the mutex that is not obtained")
      }
    }
  }

  private fun tryLockAfterResume(threadId: pthread_t) {
    while (true) {
      val state = lock.value
      val newState = if (state.waiters == 0) // deflate
        LockState(THIN, 1, 0, threadId)
      else
        LockState(FAT, 1, state.waiters, threadId, state.mutex)
      if (lock.compareAndSet(state, newState)) {
        if (state.waiters == 0) {
          state.mutex!!.unlock()
          mutexPool.release(state.mutex)
        }
        return
      }
    }
  }

  class LockState(
      val status: Status,
      val nestedLocks: Int,
      val waiters: Int,
      val ownerThreadId: pthread_t? = null,
      val mutex: CPointer<mutex_node_t>? = null
  ) {
    init { freeze() }
  }

  enum class Status { UNLOCKED, THIN, FAT }

  private fun CPointer<mutex_node_t>.lock() = lock(this.pointed.mutex)

  private fun CPointer<mutex_node_t>.unlock() = unlock(this.pointed.mutex)
}



public inline fun <T> synchronized(lock: SynchronizedObject, block: () -> T): T {
  lock.lock()
  try {
    return block()
  } finally {
    lock.unlock()
  }
}

private const val INITIAL_POOL_CAPACITY = 64

@SharedImmutable
private val mutexPool by lazy { MutexPool(INITIAL_POOL_CAPACITY) }

class MutexPool(capacity: Int) {
  private val top = AtomicNativePtr(NativePtr.NULL)

  private val mutexes = nativeHeap.allocArray<mutex_node_t>(capacity) { mutex_node_init(ptr) }

  init {
    for (i in 0 until capacity) {
      release(interpretCPointer<mutex_node_t>(mutexes.rawValue.plus(i * mutex_node_t.size))!!)
    }
  }

  private fun allocMutexNode() = nativeHeap.alloc<mutex_node_t> { mutex_node_init(ptr) }.ptr

  fun allocate(): CPointer<mutex_node_t> = pop() ?: allocMutexNode()

  fun release(mutexNode: CPointer<mutex_node_t>) {
    while (true) {
      val oldTop = interpretCPointer<mutex_node_t>(top.value)
      mutexNode.pointed.next = oldTop
      if (top.compareAndSet(oldTop.rawValue, mutexNode.rawValue))
        return
    }
  }

  private fun pop(): CPointer<mutex_node_t>? {
    while (true) {
      val oldTop = interpretCPointer<mutex_node_t>(top.value)
      if (oldTop.rawValue === NativePtr.NULL)
        return null
      val newHead = oldTop!!.pointed.next
      if (top.compareAndSet(oldTop.rawValue, newHead.rawValue))
        return oldTop
    }
  }
}