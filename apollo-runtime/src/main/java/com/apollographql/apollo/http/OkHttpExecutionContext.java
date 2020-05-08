package com.apollographql.apollo.http;

import com.apollographql.apollo.api.ApolloExperimental;
import com.apollographql.apollo.api.ExecutionContext;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * Http GraphQL execution context, provides access to the raw {@link okhttp3.Response} response.
 */
@ApolloExperimental
public class OkHttpExecutionContext implements ExecutionContext.Element {

  public static final ExecutionContext.Key<OkHttpExecutionContext> KEY = new ExecutionContext.Key<OkHttpExecutionContext>() {
  };

  /**
   * Raw OkHttp response.
   */
  public final Response response;

  public OkHttpExecutionContext(@NotNull Response response) {
    checkNotNull(response, "response == null");
    this.response = strip(response);
  }

  @NotNull @Override public ExecutionContext.Key<?> getKey() {
    return KEY;
  }

  private static @NotNull Response strip(@NotNull Response response) {
    final Response.Builder builder = response.newBuilder();

    if (response.body() != null) {
      builder.body(null);
    }

    final Response cacheResponse = response.cacheResponse();
    if (cacheResponse != null) {
      builder.cacheResponse(strip(cacheResponse));
    }

    final Response networkResponse = response.networkResponse();
    if (networkResponse != null) {
      builder.networkResponse(strip(networkResponse));
    }

    return builder.build();
  }
}
