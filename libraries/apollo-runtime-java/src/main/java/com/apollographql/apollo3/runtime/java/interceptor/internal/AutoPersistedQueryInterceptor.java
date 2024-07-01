package com.apollographql.apollo.runtime.java.interceptor.internal;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.http.HttpMethod;
import com.apollographql.apollo.exception.AutoPersistedQueriesNotSupported;
import com.apollographql.apollo.runtime.java.ApolloCallback;
import com.apollographql.apollo.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo.runtime.java.interceptor.ApolloInterceptorChain;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AutoPersistedQueryInterceptor implements ApolloInterceptor {
  private final HttpMethod httpMethodForHashedQueries;
  private final HttpMethod httpMethodForDocumentQueries;

  public AutoPersistedQueryInterceptor(
      HttpMethod httpMethodForHashedQueries,
      HttpMethod httpMethodForDocumentQueries
  ) {
    this.httpMethodForHashedQueries = httpMethodForHashedQueries;
    this.httpMethodForDocumentQueries = httpMethodForDocumentQueries;
  }

  @Override public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
    Boolean enabled = request.getEnableAutoPersistedQueries();
    if (enabled == null) {
      enabled = true;
    }

    if (!enabled) {
      chain.proceed(request, callback);
    }

    boolean isMutation = request.getOperation() instanceof Mutation;

    ApolloRequest<D> request2 = request.newBuilder()
        .httpMethod(isMutation ? HttpMethod.Post : httpMethodForHashedQueries)
        .sendDocument(false)
        .sendApqExtensions(true)
        .build();


    ApolloCallback<D> callback2 = new ApolloCallback<D>() {
      @Override public void onResponse(@NotNull ApolloResponse<D> response) {
        if (isPersistedQueryNotFound(response.errors)) {
          continueWithDocumentRequest(chain, request, callback);
        } else if (isPersistedQueryNotSupported(response.errors)) {
          callback.onResponse(new ApolloResponse.Builder<>(request.getOperation(), request.getRequestUuid())
              .exception(new AutoPersistedQueriesNotSupported())
              .build());
        } else {
          // Cache hit
          callback.onResponse(addAutoPersistedQueryInfo(response, true));
        }
      }
    };

    chain.proceed(request2, callback2);
  }

  private <D extends Operation.Data> void continueWithDocumentRequest(ApolloInterceptorChain chain, ApolloRequest<D> request, ApolloCallback<D> callback) {
    boolean isMutation = request.getOperation() instanceof Mutation;
    ApolloRequest<D> request2 = request.newBuilder()
        .httpMethod(isMutation ? HttpMethod.Post : httpMethodForDocumentQueries)
        .sendDocument(true)
        .sendApqExtensions(true)
        .build();

    ApolloCallback<D> callback2 = new ApolloCallback<D>() {
      @Override public void onResponse(@NotNull ApolloResponse<D> response) {
        callback.onResponse(addAutoPersistedQueryInfo(response, false));
      }
    };

    chain.proceed(request2, callback2);
  }

  static private <D extends Operation.Data> ApolloResponse<D> addAutoPersistedQueryInfo(ApolloResponse<D> response, boolean hit) {
    return response.newBuilder()
        .addExecutionContext(new AutoPersistedQueryInfo(hit))
        .build();
  }

  static private boolean isPersistedQueryNotFound(List<Error> errors) {
    if (errors == null) {
      return false;
    }
    for (Error error : errors) {
      if (error.getMessage().equals(PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND)) {
        return true;
      }
    }
    return false;
  }

  static private boolean isPersistedQueryNotSupported(List<Error> errors) {
    if (errors == null) {
      return false;
    }
    for (Error error : errors) {
      if (error.getMessage().equals(PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED)) {
        return true;
      }
    }
    return false;
  }

  private static final String PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND = "PersistedQueryNotFound";
  private static final String PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported";
}
