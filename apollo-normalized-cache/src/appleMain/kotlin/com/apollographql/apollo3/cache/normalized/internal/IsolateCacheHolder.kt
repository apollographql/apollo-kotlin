package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze

actual class DefaultCacheHolder actual constructor(producer: () -> OptimisticCache) {
  private val queue = dispatch_queue_create(label = "isolate-cache", null)
  private lateinit var optimisticCache: OptimisticCache

  init {
    optimisticCache.ensureNeverFrozen()

    producer.freeze()

    dispatch_async(queue) {
      initRuntimeIfNeeded()
      optimisticCache = producer()
    }
  }

  companion object {
    private fun <R> dispatchOnMain(result: Result<R>, continuation: Continuation<R>) {
      dispatch_async(dispatch_get_main_queue()) {
        continuation.resumeWith(result)
      }
    }
  }

  suspend fun <T> access(block: () -> T) = suspendCoroutine<T> { continuation ->
    continuation.freeze()

    dispatch_async(queue) {
      val result = kotlin.runCatching {
        block()
      }
      dispatchOnMain(result, continuation)
    }
  }
  actual suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return access {
      block(optimisticCache)
    }
  }

  actual suspend fun <T> write(block: (OptimisticCache) -> T): T {
    return access {
      block(optimisticCache)
    }
  }

  actual fun writeAndForget(block: (OptimisticCache) -> Unit) {
    dispatch_async(queue) {
      kotlin.runCatching {
        block(optimisticCache)
      }
      // errors are lost forever....
    }
  }
}