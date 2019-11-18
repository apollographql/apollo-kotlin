package com.apollographql.apollo.api;

/**
 * GraphQL operation name.
 */
interface OperationName {

  /**
   * Returns operation name.
   *
   * @return operation name
   */
  fun name(): String

  companion object {
    inline operator fun invoke(crossinline block: () -> String) = object : OperationName {
      override fun name(): String {
        return block()
      }
    }
  }
}
