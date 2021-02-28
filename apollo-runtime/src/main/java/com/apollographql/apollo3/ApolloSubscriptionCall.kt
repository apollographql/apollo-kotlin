package com.apollographql.apollo3

import com.apollographql.apollo3.ApolloSubscriptionCall.Callback
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.exception.ApolloCanceledException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.internal.util.Cancelable

/**
 *
 * `ApolloSubscriptionCall` is an abstraction for a request that has been prepared for subscription.
 * `ApolloSubscriptionCall`` cannot be executed twice, though it can be cancelled. Any updates pushed by
 * server related to provided subscription will be notified via [Callback]`
 *
 *
 * In order to execute the request again, call the [ApolloSubscriptionCall.clone] method which creates a new
 * `ApolloSubscriptionCall` object.
 */
interface ApolloSubscriptionCall<D : Operation.Data> : Cancelable {
  /**
   * Sends [Subscription] to the subscription server and starts listening for the pushed updates. To cancel this
   * subscription call use [.cancel].
   *
   * @param callback which will handle the subscription updates or a failure exception.
   * @throws ApolloCanceledException when the call has already been canceled
   * @throws IllegalStateException   when the call has already been executed
   */
  fun execute(callback: Callback<D>)

  /**
   * Creates a new, identical call to this one which can be executed even if this call has already been.
   *
   * @return The cloned `ApolloSubscriptionCall` object.
   */
  fun clone(): ApolloSubscriptionCall<D>

  /**
   * Sets the cache policy for response/request cache.
   *
   * @param cachePolicy [CachePolicy] to set
   * @return [ApolloSubscriptionCall] with the provided [CachePolicy]
   */
  fun cachePolicy(cachePolicy: CachePolicy): ApolloSubscriptionCall<D>

  /**
   * Factory for creating [ApolloSubscriptionCall] calls.
   */
  interface Factory {
    /**
     * Creates and prepares a new [ApolloSubscriptionCall] call.
     *
     * @param subscription to be sent to the subscription server to start listening pushed updates
     * @return prepared [ApolloSubscriptionCall] call to be executed
     */
    fun <D : Operation.Data> subscribe(
        subscription: Subscription<D>): ApolloSubscriptionCall<D>
  }

  /**
   * Subscription normalized cache policy.
   */
  enum class CachePolicy {
    /**
     * Signals the apollo subscription client to bypass normalized cache. Fetch GraphQL response from the network only and don't cache it.
     */
    NO_CACHE,

    /**
     * Signals the apollo subscription client to fetch the GraphQL response from the network only and cache it to normalized cache.
     */
    NETWORK_ONLY,

    /**
     * Signals the apollo subscription client to first fetch the GraphQL response from the cache, then fetch it from network.
     */
    CACHE_AND_NETWORK
  }

  /**
   * Communicates responses from a subscription server.
   */
  interface Callback<D : Operation.Data> {
    /**
     * Gets called when GraphQL response is received and parsed successfully. This may be called multiple times. [ ][.onCompleted] will be called after the final call to onResponse.
     *
     * @param response the GraphQL response
     */
    fun onResponse(response: ApolloResponse<D>)

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response. Will be
     * called at most one time. It is considered a terminal event. After called, neither [.onResponse]
     * or [.onCompleted] will be called again.
     */
    fun onFailure(e: ApolloException)

    /**
     * Gets called when final GraphQL response is received.  It is considered a terminal event.
     */
    fun onCompleted()

    /**
     * Gets called when GraphQL subscription server connection is closed unexpectedly. It is considered to re-try
     * the subscription later.
     */
    fun onTerminated()

    /**
     * Gets called when GraphQL subscription server connection is opened.
     */
    fun onConnected()
  }
}