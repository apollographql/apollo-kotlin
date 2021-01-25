package com.apollographql.apollo.cache.normalized.internal

internal expect object Platform {
  fun currentTimeMillis(): Long
}

expect class ReentrantReadWriteLock constructor()

internal expect inline fun <T> ReentrantReadWriteLock.read(action: () -> T): T

internal expect inline fun <T> ReentrantReadWriteLock.write(action: () -> T): T
