package com.apollographql.apollo.http;

import com.apollographql.apollo.api.ExecutionContext;
import com.apollographql.apollo.api.internal.Utils;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * Http GraphQL execution context, provides access to the raw {@link okhttp3.Response} response.
 */
public class HttpExecutionContext implements ExecutionContext.Element {

  public static final ExecutionContext.Key<HttpExecutionContext> KEY = new ExecutionContext.Key<HttpExecutionContext>() {
  };

  /**
   * Raw OkHttp response.
   */
  public final Response response;

  public HttpExecutionContext(@NotNull Response response) {
    this.response = Utils.checkNotNull(response, "response == null");
  }
}
