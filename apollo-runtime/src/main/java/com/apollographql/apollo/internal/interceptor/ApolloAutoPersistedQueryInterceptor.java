package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.ApolloLogger;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executor;

public class ApolloAutoPersistedQueryInterceptor implements ApolloInterceptor {
  private static final String PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND = "PersistedQueryNotFound";
  private static final String PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported";

  private final ApolloLogger logger;
  private volatile boolean disposed;

  public ApolloAutoPersistedQueryInterceptor(@NotNull ApolloLogger logger) {
    this.logger = logger;
  }

  @Override
  public void interceptAsync(@NotNull final InterceptorRequest request, @NotNull final ApolloInterceptorChain chain,
      @NotNull final Executor dispatcher, @NotNull final CallBack callBack) {
    InterceptorRequest newRequest = request.toBuilder().sendQueryDocument(false).build();
    chain.proceedAsync(newRequest, dispatcher, new CallBack() {
      @Override public void onResponse(@NotNull InterceptorResponse response) {
        if (disposed) return;

        Optional<InterceptorRequest> retryRequest = handleProtocolNegotiation(request, response);
        if (retryRequest.isPresent()) {
          chain.proceedAsync(retryRequest.get(), dispatcher, callBack);
        } else {
          callBack.onResponse(response);
          callBack.onCompleted();
        }
      }

      @Override public void onFetch(FetchSourceType sourceType) {
        callBack.onFetch(sourceType);
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        callBack.onFailure(e);
      }

      @Override public void onCompleted() {
        // call onCompleted in onResponse
      }
    });
  }

  @Override public void dispose() {
    disposed = true;
  }

  Optional<InterceptorRequest> handleProtocolNegotiation(final InterceptorRequest request,
      InterceptorResponse response) {
    return response.parsedResponse.flatMap(new Function<Response, Optional<InterceptorRequest>>() {
      @NotNull @Override public Optional<InterceptorRequest> apply(@NotNull Response response) {
        if (response.hasErrors()) {
          if (isPersistedQueryNotFound(response.errors())) {
            logger.w("GraphQL server couldn't find Automatic Persisted Query for operation name: "
                + request.operation.name().name() + " id: " + request.operation.operationId());
            return Optional.of(request.toBuilder().sendQueryDocument(true).build());
          }

          if (isPersistedQueryNotSupported(response.errors())) {
            // TODO how to disable Automatic Persisted Queries in future and how to notify user about this
            logger.e("GraphQL server doesn't support Automatic Persisted Queries");
            return Optional.of(request.toBuilder().sendQueryDocument(true).build());
          }
        }
        return Optional.absent();
      }
    });
  }

  boolean isPersistedQueryNotFound(List<Error> errors) {
    for (Error error : errors) {
      if (PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND.equalsIgnoreCase(error.message())) {
        return true;
      }
    }
    return false;
  }

  boolean isPersistedQueryNotSupported(List<Error> errors) {
    for (Error error : errors) {
      if (PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED.equalsIgnoreCase(error.message())) {
        return true;
      }
    }
    return false;
  }
}
