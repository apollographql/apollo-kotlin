package com.apollographql.apollo.api

import kotlinx.cinterop.StableRef
import platform.darwin.DISPATCH_QUEUE_SERIAL
import platform.darwin.dispatch_queue_create
import platform.darwin.dispatch_queue_t
import platform.darwin.dispatch_sync
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

actual class ThreadSafeMap<K, V> {
  private val stableRef = StableRef.create(mutableMapOf<K, V>())
  private val queue = dispatch_queue_create(label = "ThreadSafeMap", attr = DISPATCH_QUEUE_SERIAL as dispatch_queue_t)

  actual fun getOrPut(key: K, defaultValue: () -> V): V {
    defaultValue.freeze()
    val reference = AtomicReference<V?>(null)
    dispatch_sync(
        queue = queue,
    ) {
      initRuntimeIfNeeded()

      reference.value = stableRef.get().getOrPut(key, defaultValue).freeze()
    }

    val result = reference.value
    reference.value = null
    return result!!
  }

  fun dispose() {
    queue?.finalize()
    stableRef.dispose()
  }
}

