package com.apollographql.apollo3.api.internal

@Deprecated("This shouldn't be used anymore with Kotlin APIs")
interface Function<T, R> {
  /**
   * Apply some calculation to the input value and return some other value.
   *
   * @param t the input value
   * @return the output value
   */
  fun apply(t: T): R
}