package com.apollographql.apollo3.internal

internal actual class NonMainWorker actual constructor() {
  actual suspend fun <R> doWork(block: () -> R): R {
    return block()
  }
}