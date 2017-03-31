package com.apollographql.android;

/**
 * Represents an operation which can be cancelled.
 */
public interface Cancelable {
  /**
   * Cancels the operation.
   */
  void cancel();
}
