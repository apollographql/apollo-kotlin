package com.apollographql.apollo3.cache.normalized.internal

import kotlinx.cinterop.StableRef
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze

actual class Guard<R: Any> actual constructor(name: String, producer: () -> R) {
  private val worker = Worker.start(name = "Guard '$name'")

  private val stableRef = doWork {
    StableRef.create(producer())
  }

  private fun <T> doWork(block: () -> T): T {
    val result = worker.execute(TransferMode.SAFE, { block.freeze() }) {
      val ret = it().freeze()
      ret
    }.result

    return result
  }

  actual fun <T> access(block: (R) -> T): T {
    return doWork {
      block(stableRef.get())
    }
  }

  actual fun writeAndForget(block: (R) -> Unit) {
    doWork {
      block(stableRef.get())
    }
  }
}