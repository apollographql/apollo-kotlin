package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.sql.internal.IOTaskExecutor
import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze

@ThreadLocal
private var optimisticCache: OptimisticCache? = null

actual class DefaultCacheHolder actual constructor(producer: () -> OptimisticCache) {
  private val ioTaskExecutor = IOTaskExecutor("Cache Holder")

  init {
    ioTaskExecutor.executeAndForget {
      optimisticCache = producer()
      optimisticCache!!.ensureNeverFrozen()
    }
  }

  actual suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return ioTaskExecutor.execute {
      block(optimisticCache!!)
    }
  }

  actual suspend fun <T> write(block: (OptimisticCache) -> T): T {
    return ioTaskExecutor.execute {
      block(optimisticCache!!)
    }
  }

  actual fun writeAndForget(block: (OptimisticCache) -> Unit) {
    ioTaskExecutor.executeAndForget {
      kotlin.runCatching {
        block(optimisticCache!!)
      }
      // errors are lost forever....
    }
  }
}