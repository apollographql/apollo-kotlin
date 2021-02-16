package com.apollographql.apollo3.cache.normalized.internal

fun interface Transaction<T, R> {
  fun execute(cache: T): R?
}