package com.apollographql.apollo3.api

expect inline fun <reified T>  Any.unsafeCastOrCast(): T

inline fun <reified T> Any.defaultUnsafeCastOrCast(): T {
  return this as T
}
