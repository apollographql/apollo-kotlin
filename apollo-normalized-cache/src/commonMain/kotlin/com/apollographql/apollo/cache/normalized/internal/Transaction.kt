package com.apollographql.apollo.cache.normalized.internal

fun interface Transaction<T, R> {
  fun execute(cache: T): R?
}