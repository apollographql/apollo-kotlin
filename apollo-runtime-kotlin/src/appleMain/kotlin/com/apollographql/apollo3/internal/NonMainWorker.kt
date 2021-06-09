package com.apollographql.apollo3.internal

import com.apollographql.apollo3.mpp.GCDWorker

actual class NonMainWorker actual constructor() {
  private val gcdWorker = GCDWorker()
  actual suspend fun <R> doWork(block: () -> R): R {
    return gcdWorker.execute(block)
  }
}