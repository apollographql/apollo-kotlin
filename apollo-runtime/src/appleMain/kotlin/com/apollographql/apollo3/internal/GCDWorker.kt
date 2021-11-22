package com.apollographql.apollo3.internal

import kotlinx.cinterop.convert
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.posix.QOS_CLASS_BACKGROUND
import kotlin.native.concurrent.freeze

class GCDWorker(qos: UInt = QOS_CLASS_BACKGROUND) {
  private val queue = dispatch_get_global_queue(qos.convert(), 0)

  suspend fun <R> execute(block: () -> R) = suspendAndResumeOnMain<R> { mainContinuation, _ ->
    block.freeze()
    val callback = {
      initRuntimeIfNeeded()
      mainContinuation.resumeWith(runCatching(block))
    }

    dispatch_async(queue, callback.freeze())
  }
}