package com.apollographql.apollo.api

import kotlinx.cinterop.StableRef
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

/**
 * threadsafe Map with a minimalist API
 * inspired by https://github.com/touchlab/Stately
 */
actual class ThreadSafeMap<K, V> {
  private val worker = Worker.start(name = "ThreadSafeMap")

  private val stableRef = doWork {
    StableRef.create(mutableMapOf<K, V>())
  }

  actual fun getOrPut(key: K, defaultValue: () -> V): V {
    return doWork {
      val map = stableRef.get()
      val ret = map.getOrPut(key, defaultValue)
      ret.freeze()
    }
  }

  private fun <R> doWork(block: () -> R): R {
    val result = worker.execute(TransferMode.SAFE, { block.freeze() }) {
      val ret = it().freeze()
      ret
    }.result

    return result
  }

  actual fun dispose() {
    stableRef.dispose()
  }
}

