package com.apollographql.apollo3.api.internal

interface Action<T> {
  fun apply(t: T)
}