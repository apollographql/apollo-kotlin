package com.apollographql.apollo.interceptor;

import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.exception.ApolloException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Executor;

public class ApolloAutoPersistedOperationInterceptor implements ApolloInterceptor {
  private static final String PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND = "PersistedQueryNotFound";
  private static final String PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported";

  private final ApolloLogger logger;
  private volatile boolean disposed;

  final boolean useHttpGetMethodForPersistedOperations;

  public ApolloAutoPersistedOperationInterceptor(@NotNull ApolloLogger logger,
                                             boolean useHttpGetMethodForPersistedOperations) {
    this.logger = logger;
    this.useHttpGetMethodForPersistedOperations = useHttpGetMethodForPersistedOperations;
  }

  @Override
  public void interceptAsync(@NotNull final InterceptorRequest request, @NotNull final ApolloInterceptorChain chain,
      @NotNull final Executor dispatcher, @NotNull final CallBack callBack) {

    InterceptorRequest newRequest = request.toBuilder()
            .sendQueryDocument(false)
            .autoPersistQueries(true)
            .useHttpGetMethodForQueries(request.useHttpGetMethodForQueries || useHttpGetMethodForPersistedOperations)
            .build();
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
          if (isPersistedQueryNotFound(response.getErrors())) {
            logger.w("GraphQL server couldn't find Automatic Persisted Query for operation name: "
                + request.operation.name().name() + " id: " + request.operation.operationId());

            InterceptorRequest retryRequest = request.toBuilder()
                .autoPersistQueries(true)
                .sendQueryDocument(true)
                .build();
            return Optional.of(retryRequest);
          }

          if (isPersistedQueryNotSupported(response.getErrors())) {
            // TODO how to disable Automatic Persisted Queries in future and how to notify user about this
            logger.e("GraphQL server doesn't support Automatic Persisted Queries");
            return Optional.of(request);
          }
        }
        return Optional.absent();
      }
    });
  }

  boolean isPersistedQueryNotFound(List<Error> errors) {
    for (Error error : errors) {
      if (PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND.equalsIgnoreCase(error.getMessage())) {
        return true;
      }
    }
    return false;
  }

  boolean isPersistedQueryNotSupported(List<Error> errors) {
    for (Error error : errors) {
      if (PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED.equalsIgnoreCase(error.getMessage())) {
        return true;
      }
    }
    return false;
  }

  public static class Factory implements ApolloInterceptorFactory {

    final boolean useHttpGet;
    final boolean persistQueries;
    final boolean persistMutations;

    public Factory(boolean useHttpGet, boolean persistQueries, boolean persistMutations) {
      this.useHttpGet = useHttpGet;
      this.persistQueries = persistQueries;
      this.persistMutations = persistMutations;
    }

    public Factory() {
      this(false, true, true);
    }

    @Nullable @Override public ApolloInterceptor newInterceptor(@NotNull ApolloLogger logger, @NotNull Operation<?> operation) {
      if (operation instanceof Query && !persistQueries) {
        return null;
      }
      if (operation instanceof Mutation && !persistMutations) {
        return null;
      }
      return new ApolloAutoPersistedOperationInterceptor(logger, useHttpGet);
    }
  }
}
