package com.apollographql.apollo;

/**
 * Callback which gets invoked when the {@link ApolloClient} transitions
 * from active to idle state.
 */
public interface IdleCallback {

  /**
   * Gets called when the apolloClient transitions from active to idle state.
   */
  void onIdle();

}
