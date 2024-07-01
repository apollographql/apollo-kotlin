package com.apollographql.apollo.api

actual inline fun <reified T> Any.apolloUnsafeCast(): T {
  return this.unsafeCast<T>()
}
