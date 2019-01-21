package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import org.jetbrains.annotations.NotNull;

/**
 * <p>{@code ApolloSubscriptionCall} is an abstraction for a request that has been prepared for subscription.
 * <code>ApolloSubscriptionCall<code/> cannot be executed twice, though it can be cancelled. Any updates pushed by
 * server related to provided subscription will be notified via {@link Callback}</p>
 *
 * <p>In order to execute the request again, call the {@link ApolloSubscriptionCall#clone()} method which creates a new
 * {@code ApolloSubscriptionCall} object.</p>
 */
public interface ApolloSubscriptionCall<T> extends Cancelable {

  /**
   * Sends {@link Subscription} to the subscription server and starts listening for the pushed updates. To cancel this
   * subscription call use {@link #cancel()}.
   *
   * @param callback which will handle the subscription updates or a failure exception.
   * @throws ApolloCanceledException when the call has already been canceled
   * @throws IllegalStateException   when the call has already been executed
   */
  void execute(@NotNull Callback<T> callback);

  /**
   * Creates a new, identical call to this one which can be executed even if this call has already been.
   *
   * @return The cloned {@code ApolloSubscriptionCall} object.
   */
  ApolloSubscriptionCall<T> clone();

  /**
   * Factory for creating {@link ApolloSubscriptionCall} calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link ApolloSubscriptionCall} call.
     *
     * @param subscription to be sent to the subscription server to start listening pushed updates
     * @return prepared {@link ApolloSubscriptionCall} call to be executed
     */
    <D extends Subscription.Data, T, V extends Subscription.Variables> ApolloSubscriptionCall<T> subscribe(
        @NotNull Subscription<D, T, V> subscription);
  }

  /**
   * Communicates responses from a subscription server.
   */
  interface Callback<T> {

    /**
     * Gets called when GraphQL response is received and parsed successfully. This may be called multiple times. {@link
     * #onCompleted()} will be called after the final call to onResponse.
     *
     * @param response the GraphQL response
     */
    void onResponse(@NotNull Response<T> response);

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response. Will be
     * called at most one time. It is considered a terminal event. After called, neither {@link #onResponse(Response)}
     * or {@link #onCompleted()} will be called again.
     */
    void onFailure(@NotNull ApolloException e);

    /**
     * Gets called when final GraphQL response is received.  It is considered a terminal event.
     */
    void onCompleted();

    /**
     * Gets called when GraphQL subscription server connection is closed unexpectedly. It is considered to re-try
     * the subscription later.
     */
    void onTerminated();
  }
}
