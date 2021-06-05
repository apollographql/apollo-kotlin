package com.apollographql.apollo3.mpp

import kotlinx.cinterop.convert
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.posix.QOS_CLASS_BACKGROUND
import platform.posix.intptr_t
import kotlin.native.concurrent.freeze

class GCDWorker(qos: UInt = QOS_CLASS_BACKGROUND) {
  private val queue = dispatch_get_global_queue(qos.convert(), 0)

  suspend fun <R> execute(block: () -> R) = suspendAndResumeOnMain<R> { mainContinuation, _ ->
    block.freeze()

    dispatch_async(queue) {
      mainContinuation.resumeWith(runCatching(block))
    }
  }
}