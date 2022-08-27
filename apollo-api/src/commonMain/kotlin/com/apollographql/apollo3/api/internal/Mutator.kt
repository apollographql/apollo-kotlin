package com.apollographql.apollo3.api.internal

/**
 * Represents an operation that accepts a single input argument, mutate it and returns no result
 *
 * @param <T> the type of the input to the operation
</T> */
interface Mutator<T> {
  /**
   * Performs this operation on the given argument.
   *
   * @param t the input argument
   */
  fun accept(t: T)
}
