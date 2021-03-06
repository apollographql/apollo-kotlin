package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.cache.normalized.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.sql.internal.IOTaskExecutor
import com.apollographql.apollo3.cache.normalized.sql.internal.currentThreadId
import kotlinx.cinterop.StableRef
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze

actual class DefaultCacheHolder actual constructor(producer: () -> OptimisticCache) {
  private val worker = Worker.start(name = "Cache Holder")

  private val stableRef = doWork {
    StableRef.create(producer())
  }

  private fun <R> doWork(block: () -> R): R {
    val result = worker.execute(TransferMode.SAFE, { block.freeze() }) {
      val ret = it().freeze()
      ret
    }.result

    return result
  }

  actual suspend fun <T> read(block: (ReadOnlyNormalizedCache) -> T): T {
    return doWork {
      val optimisticCache = stableRef.get()
      block(optimisticCache)
    }
  }

  actual suspend fun <T> write(block: (OptimisticCache) -> T): T {
    return doWork {
      val optimisticCache = stableRef.get()
      block(optimisticCache)
    }
  }

  actual fun writeAndForget(block: (OptimisticCache) -> Unit) {
    doWork {
      val optimisticCache = stableRef.get()
      block(optimisticCache)
    }
  }
}