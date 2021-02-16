package com.apollographql.apollo3.internal.util

/**
 * Represents an operation which can be canceled.
 */
interface Cancelable {
  /**
   * Cancels the operation.
   */
  fun cancel()

  /**
   * Checks if this operation has been canceled.
   *
   * @return true if this operation has been canceled else returns false
   */
  val isCanceled: Boolean
}