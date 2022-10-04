package com.apollographql.apollo3.runtime.java.internal;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.exception.AutoPersistedQueriesNotSupported;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.ApolloInterceptorChain;
import com.apollographql.apollo3.api.Error;
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
          callback.onFailure(new AutoPersistedQueriesNotSupported());
        } else {
          // Cache hit
          callback.onResponse(addAutoPersistedQueryInfo(response, true));
        }
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        callback.onFailure(e);
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

      @Override public void onFailure(@NotNull ApolloException e) {
        callback.onFailure(e);
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
