package com.apollographql.apollo3.api

actual inline fun <reified T> Any.unsafeCastOrCast(): T? {
  return this.unsafeCast<T>()
}
