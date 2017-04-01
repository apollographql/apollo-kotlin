package com.apollographql.apollo;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import okhttp3.Response;

/**
 * <p>ApolloPrefetch is an abstraction for a request that has been prepared for execution. It fetches the graph
 * response from the server on successful completion but <b>doesn't</b> inflate the response into models. Instead it
 * stores the raw response in the request/response cache and defers the parsing to a later time.</p>
 *
 *
 * <p>Use this object for use cases when the data needs to be fetched, but is not required for immediate
 * consumption. e.g.background update/syncing.</p>
 */
public interface ApolloPrefetch extends Cancelable {

  /**
   * Sends the request immediately and blocks until the response can be processed or is an error.
   *
   * @throws ApolloException       if the request could not be executed due to a cancellation, a timeout or a network
   *                               failure
   * @throws IllegalStateException when the call has already been executed
   */
  void execute() throws ApolloException;

  /**
   * Schedules the request to be executed at some point in the future.
   * The dispatcher defines when the request will run: usually immediately unless there are several other requests
   * currently being executed.
   *
   * @param callback Callback which will handle the success response or a failure exception
   * @throws IllegalStateException when the call has already been executed
   */
  @Nonnull ApolloPrefetch enqueue(@Nullable Callback callback);

  /**
   * Creates a new, identical ApolloPrefetch to this one which can be enqueued or executed even if this one has
   * already been executed.
   *
   * @return The cloned ApolloPrefetch object
   */
  ApolloPrefetch clone();

  /**
   * Communicates responses from the server.
   */
  abstract class Callback {

    /**
     * Gets called when the request has succeeded.
     */
    public abstract void onSuccess();

    /**
     * Gets called when a network exception occurs while communicating with the server, or an unexpected
     * exception occurs while creating the request or processing the response.
     */
    public abstract void onFailure(@Nonnull ApolloException e);

    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
      Response response = e.rawResponse();
      if (response != null) {
        response.close();
      }
    }

    public void onNetworkError(@Nonnull ApolloNetworkException e) {
      onFailure(e);
    }
  }
}
