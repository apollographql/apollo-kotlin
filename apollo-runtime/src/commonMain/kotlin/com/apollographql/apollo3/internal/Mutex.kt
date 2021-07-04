package com.apollographql.apollo3.internal

interface Mutex {
  fun <T> lock(block: () -> T): T
}

object NoOpMutex: Mutex {
  override fun <T> lock(block: () -> T): T {
    return block()
  }
}