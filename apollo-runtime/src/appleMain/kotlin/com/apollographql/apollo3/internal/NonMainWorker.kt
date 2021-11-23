package com.apollographql.apollo3.internal

internal actual class NonMainWorker actual constructor() {
  private val gcdWorker = GCDWorker()
  actual suspend fun <R> doWork(block: () -> R): R {
    return gcdWorker.execute(block)
  }
}