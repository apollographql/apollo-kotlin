package com.apollographql.apollo.api.internal

interface Action<T> {
  fun apply(t: T)
}