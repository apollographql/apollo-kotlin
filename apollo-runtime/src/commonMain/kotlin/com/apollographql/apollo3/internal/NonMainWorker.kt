package com.apollographql.apollo3.internal

internal expect class NonMainWorker() {
  suspend fun <R> doWork(block: () -> R): R
}