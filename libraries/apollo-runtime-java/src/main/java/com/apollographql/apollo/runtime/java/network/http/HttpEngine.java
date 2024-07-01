package com.apollographql.apollo.runtime.java.network.http;

import com.apollographql.apollo.api.http.HttpRequest;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import org.jetbrains.annotations.NotNull;

public interface HttpEngine {
  /**
   * Executes the given HttpRequest
   *
   * @param disposable a disposable that can be used to cancel the request
   */
  void execute(@NotNull HttpRequest request, @NotNull HttpCallback callback, @NotNull ApolloDisposable disposable);

  /**
   * Disposes any resources used by the HttpEngine
   * <p>
   * Use this to dispose a connection pool for example. Must be idempotent
   */
  void dispose();
}
