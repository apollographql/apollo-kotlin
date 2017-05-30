package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>ApolloCall is an abstraction for a request that has been prepared for execution. ApolloCall represents a single
 * request/response pair and cannot be executed twice, though it can be cancelled.</p>
 *
 * <p>In order to execute the request again, call the {@link ApolloCall#clone()} method which creates a new ApolloCall
 * object.</p>
 */
public interface ApolloCall<T> extends Cancelable {

  /**
   * Sends the request immediately and blocks until the response can be processed or is an error.
   *
   * @return The successful or failed {@link Response}
   * @throws ApolloException       if the request could not be executed due to a cancellation, a timeout, network
   *                               failure or a parsing error
   * @throws IllegalStateException when the call has already been executed
   */
  @Nonnull Response<T> execute() throws ApolloException;

  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the response or a failure exception.
   * @throws IllegalStateException when the call has already been executed
   */
  void enqueue(@Nullable Callback<T> callback);

  /**
   * Sets the {@link CacheControl} strategy for an ApolloCall object.
   *
   * @param cacheControl the CacheControl strategy to set
   * @return The ApolloCall object with the provided CacheControl strategy
   */
  @Nonnull ApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl);

  /**
   * Sets the {@link CacheHeaders} to use for this call.
   *
   * @param cacheHeaders the {@link CacheHeaders} that will be passed with records generated from this request to {@link
   *                     com.apollographql.apollo.cache.normalized.NormalizedCache}. Standardized cache headers are
   *                     defined in {@link ApolloCacheHeaders}.
   * @return The ApolloCall object with the provided {@link CacheHeaders}.
   */
  @Nonnull ApolloCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders);

  /**
   * Creates a new, identical call to this one which can be enqueued or executed even if this call has already been.
   *
   * @return The cloned ApolloCall object.
   */
  @Nonnull ApolloCall<T> clone();

  /**
   * Communicates responses from a server or offline requests.
   */
  abstract class Callback<T> {

    /**
     * Gets called when GraphQl response is received and parsed successfully.
     *
     * @param response the GraphQl response
     */
    public abstract void onResponse(@Nonnull Response<T> response);

    /**
     * Gets called when an unexpected exception occurs while creating the request or processing the response.
     */
    public abstract void onFailure(@Nonnull ApolloException e);

    /**
     * Gets called when an http request error takes place. This is the case when the returned http status code doesn't
     * lie in the range 200 (inclusive) and 300 (exclusive).
     */
    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
      okhttp3.Response response = e.rawResponse();
      if (response != null) {
        response.close();
      }
    }

    /**
     * Gets called when an http request error takes place due to network failures, timeouts etc.
     */
    public void onNetworkError(@Nonnull ApolloNetworkException e) {
      onFailure(e);
    }

    /**
     * Gets called when the network request succeeds but there was an error parsing the response.
     */
    public void onParseError(@Nonnull ApolloParseException e) {
      onFailure(e);
    }

    /**
     * Gets called when {@link ApolloCall} has been canceled.
     */
    public void onCanceledError(@Nonnull ApolloCanceledException e) {
      onFailure(e);
    }
  }
}
