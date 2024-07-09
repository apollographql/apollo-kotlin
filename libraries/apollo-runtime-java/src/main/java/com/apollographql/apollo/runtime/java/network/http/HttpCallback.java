package com.apollographql.apollo.runtime.java.network.http;

import com.apollographql.apollo.api.http.HttpResponse;
import com.apollographql.apollo.exception.ApolloNetworkException;
import org.jetbrains.annotations.NotNull;

/**
 * A callback for the execution of HTTP requests.
 * <p>
 * {@link #onFailure} is called if a network error happens. HTTP errors won't trigger {@link #onFailure} but
 * {@link #onResponse(HttpResponse)} with an {@link HttpResponse} indicating the status code.
 */
public interface HttpCallback {
  void onResponse(@NotNull HttpResponse response);

  void onFailure(@NotNull ApolloNetworkException exception);
}
