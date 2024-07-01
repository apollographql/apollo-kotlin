package com.apollographql.apollo.runtime.java.network.http;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Operations;
import com.apollographql.apollo.api.http.HttpRequest;
import com.apollographql.apollo.api.http.HttpRequestComposer;
import com.apollographql.apollo.api.http.HttpResponse;
import com.apollographql.apollo.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.runtime.java.ApolloCallback;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import com.apollographql.apollo.runtime.java.network.NetworkTransport;
import com.apollographql.apollo.runtime.java.network.http.internal.DefaultHttpInterceptorChain;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HttpNetworkTransport implements NetworkTransport {
  private final HttpRequestComposer requestComposer;
  private final HttpEngine httpEngine;
  private final List<HttpInterceptor> interceptors;
  private final boolean exposeErrorBody;

  public HttpNetworkTransport(
      HttpRequestComposer httpRequestComposer,
      HttpEngine httpEngine,
      List<HttpInterceptor> interceptors,
      boolean exposeErrorBody
  ) {
    this.requestComposer = httpRequestComposer;
    this.httpEngine = httpEngine;
    this.interceptors = interceptors;
    this.exposeErrorBody = exposeErrorBody;
  }

  @Override
  public <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable) {
    List<HttpInterceptor> interceptorsWithEngine = new ArrayList<>(interceptors);
    interceptorsWithEngine.add((req, chain, httpCallback) -> httpEngine.execute(req, httpCallback, disposable));
    DefaultHttpInterceptorChain chain = new DefaultHttpInterceptorChain(interceptorsWithEngine, 0);
    HttpRequest httpRequest = requestComposer.compose(request);
    chain.proceed(httpRequest, new HttpCallback() {
      @Override public void onResponse(@NotNull HttpResponse response) {
        if (response.getStatusCode() < 200 || response.getStatusCode() > 299) {
          String message = "Http request failed with status code `" + response.getStatusCode() + "`";
          if (exposeErrorBody) {
            callback.onResponse(getExceptionResponse(request, new ApolloHttpException(response.getStatusCode(), response.getHeaders(), response.getBody(), message, null)));
          } else {
            try {
              response.getBody().close();
            } catch (IOException ignored) {
            }
            callback.onResponse(getExceptionResponse(request, new ApolloHttpException(response.getStatusCode(), response.getHeaders(), null, message, null)));
          }
        } else {
          BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(response.getBody());
          CustomScalarAdapters customScalarAdapters = request.getExecutionContext().get(CustomScalarAdapters.Key);
          ApolloResponse<D> apolloResponse = Operations.toApolloResponse(jsonReader, request.getOperation(), request.getRequestUuid(), customScalarAdapters, null);
          callback.onResponse(apolloResponse);
        }
      }

      @Override public void onFailure(@NotNull ApolloNetworkException exception) {
        callback.onResponse(getExceptionResponse(request, exception));
      }
    });
  }

  @NotNull private static <D extends Operation.Data> ApolloResponse<D> getExceptionResponse(@NotNull ApolloRequest<D> request, @NotNull ApolloException exception) {
    return new ApolloResponse.Builder<>(request.getOperation(), request.getRequestUuid())
        .exception(exception)
        .build();
  }

  @Override public void dispose() {
    httpEngine.dispose();
  }
}
