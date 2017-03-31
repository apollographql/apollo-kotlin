package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.internal.util.Cancelable;
import com.apollographql.apollo.cache.http.HttpCacheControl;
import com.apollographql.apollo.cache.normalized.CacheControl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ApolloCall is an abstraction for a request that has been prepared for execution. ApolloCall
 * represents a single request/response pair and cannot be executed twice, though it can be cancelled.
 */
public interface ApolloCall<T> extends Cancelable {

  /**
   * Sends the request immediately and blocks until the response can be processed or is an error.
   *
   * @return The successful or failed {@link Response}
   * @throws IOException if the request could not be executed due to a cancellation, a timeout or a network failure
   * @throws IllegalStateException when the call has already been executed
   */
  @Nonnull Response<T> execute() throws IOException;

  /**
   * Schedules the request to be executed at some point in the future.
   * The dispatcher defines when the request will run: usually immediately unless there are several other requests
   * currently being executed.
   *
   * @param callback Callback which will handle the response or a failure exception.
   * @throws IllegalStateException when the call has already been executed
   */
  void enqueue(@Nullable Callback<T> callback);

  /**
   * Returns a watcher to watch the changes made by this call to the normalized cache store.
   */
  @Nonnull ApolloWatcher<T> watcher();

  /**
   * Sets the {@link HttpCacheControl} strategy for an ApolloCall object.
   *
   * @param httpCacheControl the HttpCacheControl strategy to set
   * @return The ApolloCall object with the provided HttpCacheControl strategy
   */
  @Nonnull ApolloCall<T> httpCacheControl(@Nonnull HttpCacheControl httpCacheControl);

  /**
   * Sets the {@link CacheControl} strategy for an ApolloCall object.
   *
   * @param cacheControl the CacheControl strategy to set
   * @return The ApolloCall object with the provided CacheControl strategy
   */
  @Nonnull ApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl);

  /**
   * Creates a new, identical call to this one which can be enqueued or executed even if this call
   * has already been.
   *
   * @return The cloned ApolloCall object.
   */
  @Nonnull ApolloCall<T> clone();

  /**
   * Communicates responses from a server or offline requests.
   */
  interface Callback<T> {

    /**
     * Gets called when GraphQl response is received.
     *
     * @param response the GraphQl response
     */
    void onResponse(@Nonnull Response<T> response);

    /**
     * Gets called when a network exception occurs while communicating with the server, or an unexpected
     * exception occurs while creating the request or processing the response.
     */
    void onFailure(@Nonnull Throwable t);
  }

  /**
   * Factory for creating ApolloCall & ApolloPrefetch objects.
   */
  interface Factory {
    /**
     * Creates the ApolloCall by wrapping the operation object inside.
     *
     * @param operation the operation which needs to be performed
     * @return The ApolloCall object with the wrapped operation object
     */
    <D extends Operation.Data, T, V extends Operation.Variables> ApolloCall<T> newCall(
        @Nonnull Operation<D, T, V> operation);

    /**
     * Creates the ApolloPrefetch by wrapping the operation object inside.
     *
     * @param operation the operation which needs to be performed
     * @return The ApolloPrefetch object with the wrapped operation object
     */
    <D extends Operation.Data, T, V extends Operation.Variables> ApolloPrefetch prefetch(
        @Nonnull Operation<D, T, V> operation);
  }
}
