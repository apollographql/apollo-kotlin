package com.apollographql.apollo3.cache.normalized.internal

import kotlin.js.Date

internal actual object Platform {
  actual fun currentTimeMillis(): Long {
    return Date().getTime().toLong()
  }
}

actual class ReentrantReadWriteLock actual constructor()

internal actual inline fun <T> ReentrantReadWriteLock.access(action: () -> T): T {
  return action()
}

internal actual inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T {
  return action()
}