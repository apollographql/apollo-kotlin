package com.apollographql.apollo3

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.exception.ApolloCanceledException
import com.apollographql.apollo3.api.exception.ApolloException
import com.apollographql.apollo3.api.exception.ApolloHttpException
import com.apollographql.apollo3.api.exception.ApolloNetworkException
import com.apollographql.apollo3.internal.util.Cancelable

/**
 *
 * ApolloPrefetch is an abstraction for a request that has been prepared for execution. It represents a single
 * request/response pair and cannot be executed twice, though it can be cancelled. It fetches the graph response from
 * the server on successful completion but **doesn't** inflate the response into models. Instead it stores the raw
 * response in the request/response cache and defers the parsing to a later time.
 *
 *
 *
 * Use this object for use cases when the data needs to be fetched, but is not required for immediate consumption.
 * e.g.background update/syncing.
 *
 *
 * Note: In order to execute the request again, call the [ApolloPrefetch.clone] method which creates a new
 * [ApolloPrefetch] object.
 */
interface ApolloPrefetch : Cancelable {
  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the success response or a failure exception
   * @throws IllegalStateException when the call has already been executed
   */
  fun enqueue(callback: Callback?)

  /**
   * Creates a new, identical ApolloPrefetch to this one which can be enqueued or executed even if this one has already
   * been executed.
   *
   * @return The cloned ApolloPrefetch object
   */
  fun clone(): ApolloPrefetch?

  /**
   * Returns GraphQL operation this call executes
   *
   * @return [Operation]
   */
  fun operation(): Operation<*>

  /**
   * Cancels this [ApolloPrefetch]. If the call has already completed, nothing will happen.
   * If the call is outgoing, an [ApolloCanceledException] will be thrown if the call was started
   * with [.execute]. If the call was started with [.enqueue]
   * the [com.apollographql.apollo3.ApolloPrefetch.Callback] will be disposed, and will receive no more events.
   * The call will attempt to abort and release resources, if possible.
   */
  override fun cancel()

  /**
   * Communicates responses from the server.
   */
  abstract class Callback {
    /**
     * Gets called when the request has succeeded.
     */
    abstract fun onSuccess()

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response.
     */
    abstract fun onFailure(e: ApolloException)

    /**
     * Gets called when an http request error takes place. This is the case when the returned http status code doesn't
     * lie in the range 200 (inclusive) and 300 (exclusive).
     */
    open fun onHttpError(e: ApolloHttpException) {
      onFailure(e)
    }

    /**
     * Gets called when an http request error takes place due to network failures, timeouts etc.
     */
    open fun onNetworkError(e: ApolloNetworkException) {
      onFailure(e)
    }

    /**
     * Gets called when [ApolloCall] has been canceled.
     */
    open fun onCanceledError(e: ApolloCanceledException) {
      onFailure(e)
    }
  }

  /**
   * Factory for creating ApolloPrefetch object.
   */
  interface Factory {
    /**
     * Creates the ApolloPrefetch by wrapping the operation object inside.
     *
     * @param operation the operation which needs to be performed
     * @return The ApolloPrefetch object with the wrapped operation object
     */
    fun <D : Operation.Data> prefetch(
        operation: Operation<D>): ApolloPrefetch
  }
}