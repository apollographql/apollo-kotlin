package com.apollographql.apollo3.mpp

expect class Guard<R: Any>(name: String, producer: () -> R) {
  suspend fun <T> access(block: (R) -> T): T

  fun <T> blockingAccess(block: (R) -> T): T

  fun writeAndForget(block: (R) -> Unit)

  fun dispose()
}