package com.apollographql.apollo3.api

actual inline fun <reified T> Any.apolloUnsafeCast(): T {
  return this.unsafeCast<T>()
}
