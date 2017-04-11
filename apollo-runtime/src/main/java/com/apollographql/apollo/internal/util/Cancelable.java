package com.apollographql.apollo.internal.util;

/**
 * Represents an operation which can be canceled.
 */
public interface Cancelable {
  /**
   * Cancels the operation.
   */
  void cancel();

  /**
   * Checks if this operation has been canceled.
   *
   * @return true if this operation has been canceled else returns false
   */
  boolean isCanceled();
}
