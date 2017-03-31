package com.apollographql.apollo.internal.util;

/**
 * Represents an operation which can be cancelled.
 */
public interface Cancelable {
  /**
   * Cancels the operation.
   */
  void cancel();
}
