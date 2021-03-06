package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.sql.internal.IOTaskExecutor
import com.apollographql.apollo3.cache.normalized.sql.internal.currentThreadId
import kotlin.native.concurrent.ensureNeverFrozen

@ThreadLocal
private var optimisticCache: OptimisticCache? = null

actual class DefaultCacheHolder actual constructor(producer: () -> OptimisticCache) {
  private val ioTaskExecutor = IOTaskExecutor("Cache Holder")

  init {
    ioTaskExecutor.executeAndForget {
      println("produce 1 ${currentThreadId()}")
      optimisticCache = producer()
      optimisticCache!!.ensureNeverFrozen()
    }
  }

  actual suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return ioTaskExecutor.execute {
      println("produce 2 ${currentThreadId()}")
      block(optimisticCache!!)
    }
  }

  actual suspend fun <T> write(block: (OptimisticCache) -> T): T {
    return ioTaskExecutor.execute {
      println("produce 3 ${currentThreadId()}")
      block(optimisticCache!!)
    }
  }

  actual fun writeAndForget(block: (OptimisticCache) -> Unit) {
    ioTaskExecutor.executeAndForget {
      println("produce 4 ${currentThreadId()}")
      kotlin.runCatching {
        block(optimisticCache!!)
      }
      // errors are lost forever....
    }
  }
}