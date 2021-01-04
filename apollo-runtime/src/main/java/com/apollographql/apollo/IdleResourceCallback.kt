package com.apollographql.apollo

/**
 * Callback which gets invoked when the resource transitions
 * from active to idle state.
 */
interface IdleResourceCallback {
  /**
   * Gets called when the resource transitions from active to idle state.
   */
  fun onIdle()
}