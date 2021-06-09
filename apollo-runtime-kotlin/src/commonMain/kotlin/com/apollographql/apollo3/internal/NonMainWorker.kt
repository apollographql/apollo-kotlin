package com.apollographql.apollo3.internal

expect class NonMainWorker() {
  suspend fun <R> doWork(block: () -> R): R
}