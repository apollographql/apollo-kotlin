package com.apollographql.apollo3.api.internal.json

/**
 * A custom [Closeable] interface so that we can use [use]{} in JS and Kotlin native
 */
expect interface Closeable {
  fun close()
}
