package com.apollographql.apollo3.api

expect inline fun <reified T>  Any.apolloUnsafeCast(): T

/**
 * Uses `unsafeCast` on JS or a regular `as?` cast on other platforms. Note that
 * this means that JS platforms will crash at runtime if the cast fails, whereas
 * other (statically) typed targets will return a null value.
 *
 * @return
 */
inline fun <reified T> Any.defaultApolloUnsafeCast(): T {
  return this as T
}
