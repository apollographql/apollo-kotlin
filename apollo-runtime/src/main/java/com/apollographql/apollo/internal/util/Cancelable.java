package com.apollographql.apollo.internal.util;

/**
 * Represents an operation which can be cancelled.
 */
public interface Cancelable {
  /**
   * Cancels the operation.
   */
  void cancel();

  /**
   * Checks if this operation has been cancelled.
   *
   * @return true if this operation has been cancelled else returns false
   */
  boolean isCanceled();
}
