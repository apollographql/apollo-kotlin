package com.apollographql.apollo.cache.normalized.internal

interface Transaction<T, R> {
  fun execute(cache: T): R?
}