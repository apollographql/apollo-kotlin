package com.apollographql.apollo;

/**
 * Callback which gets invoked when the resource transitions
 * from active to idle state.
 */
public interface IdleResourceCallback {

  /**
   * Gets called when the resource transitions from active to idle state.
   */
  void onIdle();

}
