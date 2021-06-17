package com.apollographql.apollo3.api.internal

@Deprecated("This shouldn't be used anymore with Kotlin APIs")
interface Action<T> {
  fun apply(t: T)
}