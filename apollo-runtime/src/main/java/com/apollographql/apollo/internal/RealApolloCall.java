package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloMutationCall;
import com.apollographql.apollo.ApolloQueryCall;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.Action;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.ApolloInterceptorFactory;
import com.apollographql.apollo.interceptor.ApolloAutoPersistedOperationInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloCacheInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloParseInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.apollographql.apollo.request.RequestHeaders;
import okhttp3.Call;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.CallState.ACTIVE;
import static com.apollographql.apollo.internal.CallState.CANCELED;
import static com.apollographql.apollo.internal.CallState.IDLE;
import static com.apollographql.apollo.internal.CallState.TERMINATED;
import static java.util.Collections.emptyList;

@SuppressWarnings("WeakerAccess")
public final class RealApolloCall<D extends Query.Data> implements ApolloQueryCall<D>, ApolloMutationCall<D> {
  final Operation<D, ?> operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final HttpCache httpCache;
  final HttpCachePolicy.Policy httpCachePolicy;
  final ResponseFieldMapperFactory responseFieldMapperFactory;
  final CustomScalarAdapters customScalarAdapters;
  final ApolloStore apolloStore;
  final CacheHeaders cacheHeaders;
  final RequestHeaders requestHeaders;
  final ResponseFetcher responseFetcher;
  final ApolloInterceptorChain interceptorChain;
  final Executor dispatcher;
  final ApolloLogger logger;
  final ApolloCallTracker tracker;
  final List<ApolloInterceptor> applicationInterceptors;
  final List<ApolloInterceptorFactory> applicationInterceptorFactories;
  final ApolloInterceptorFactory autoPersistedOperationsInterceptorFactory;
  final List<OperationName> refetchQueryNames;
  final List<Query> refetchQueries;
  final Optional<QueryReFetcher> queryReFetcher;
  final boolean enableAutoPersistedQueries;
  final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  final AtomicReference<Callback<D>> originalCallback = new AtomicReference<>();
  final Optional<Operation.Data> optimisticUpdates;
  final boolean useHttpGetMethodForQueries;
  final boolean useHttpGetMethodForPersistedQueries;
  final boolean writeToNormalizedCacheAsynchronously;

  public static <D extends Query.Data> Builder<D> builder() {
    return new Builder<>();
  }

  RealApolloCall(Builder<D> builder) {
    operation = builder.operation;
    serverUrl = builder.serverUrl;
    httpCallFactory = builder.httpCallFactory;
    httpCache = builder.httpCache;
    httpCachePolicy = builder.httpCachePolicy;
    responseFieldMapperFactory = builder.responseFieldMapperFactory;
    customScalarAdapters = builder.customScalarAdapters;
    apolloStore = builder.apolloStore;
    responseFetcher = builder.responseFetcher;
    cacheHeaders = builder.cacheHeaders;
    requestHeaders = builder.requestHeaders;
    dispatcher = builder.dispatcher;
    logger = builder.logger;
    applicationInterceptors = builder.applicationInterceptors;
    applicationInterceptorFactories = builder.applicationInterceptorFactories;
    autoPersistedOperationsInterceptorFactory = builder.autoPersistedOperationsInterceptorFactory;
    refetchQueryNames = builder.refetchQueryNames;
    refetchQueries = builder.refetchQueries;
    tracker = builder.tracker;

    if ((refetchQueries.isEmpty() && refetchQueryNames.isEmpty()) || builder.apolloStore == null) {
      queryReFetcher = Optional.absent();
    } else {
      queryReFetcher = Optional.of(QueryReFetcher.builder()
          .queries(builder.refetchQueries)
          .queryWatchers(refetchQueryNames)
          .serverUrl(builder.serverUrl)
          .httpCallFactory(builder.httpCallFactory)
          .responseFieldMapperFactory(builder.responseFieldMapperFactory)
          .scalarTypeAdapters(builder.customScalarAdapters)
          .apolloStore(builder.apolloStore)
          .dispatcher(builder.dispatcher)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .applicationInterceptorFactories(builder.applicationInterceptorFactories)
          .autoPersistedOperationsInterceptorFactory(builder.autoPersistedOperationsInterceptorFactory)
          .callTracker(builder.tracker)
          .build());
    }
    useHttpGetMethodForQueries = builder.useHttpGetMethodForQueries;
    enableAutoPersistedQueries = builder.enableAutoPersistedQueries;
    useHttpGetMethodForPersistedQueries = builder.useHttpGetMethodForPersistedQueries;
    optimisticUpdates = builder.optimisticUpdates;
    writeToNormalizedCacheAsynchronously = builder.writeToNormalizedCacheAsynchronously;
    interceptorChain = prepareInterceptorChain(operation);
  }

  @Override public void enqueue(@Nullable final Callback<D> responseCallback) {
    try {
      activate(Optional.fromNullable(responseCallback));
    } catch (ApolloCanceledException e) {
      if (responseCallback != null) {
        responseCallback.onCanceledError(e);
      } else {
        logger.e(e, "Operation: %s was canceled", operation().name().name());
      }
      return;
    }

    ApolloInterceptor.InterceptorRequest request = ApolloInterceptor.InterceptorRequest.builder(operation)
        .cacheHeaders(cacheHeaders)
        .requestHeaders(requestHeaders)
        .fetchFromCache(false)
        .optimisticUpdates(optimisticUpdates)
        .useHttpGetMethodForQueries(useHttpGetMethodForQueries)
        .build();
    interceptorChain.proceedAsync(request, dispatcher, interceptorCallbackProxy());
  }

  @NotNull @Override public RealApolloQueryWatcher<D> watcher() {
    return new RealApolloQueryWatcher<>(clone(), apolloStore, logger, tracker, ApolloResponseFetchers.CACHE_FIRST);
  }

  @NotNull @Override public RealApolloCall<D> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .httpCachePolicy(checkNotNull(httpCachePolicy, "httpCachePolicy == null"))
        .build();
  }

  @NotNull @Override public RealApolloCall<D> responseFetcher(@NotNull ResponseFetcher fetcher) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .responseFetcher(checkNotNull(fetcher, "responseFetcher == null"))
        .build();
  }

  @NotNull @Override public RealApolloCall<D> cacheHeaders(@NotNull CacheHeaders cacheHeaders) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .cacheHeaders(checkNotNull(cacheHeaders, "cacheHeaders == null"))
        .build();
  }

  @NotNull @Override public RealApolloCall<D> requestHeaders(@NotNull RequestHeaders requestHeaders) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .requestHeaders(checkNotNull(requestHeaders, "requestHeaders == null"))
        .build();
  }

  @Override public synchronized void cancel() {
    switch (state.get()) {
      case ACTIVE:
        state.set(CANCELED);
        try {
          interceptorChain.dispose();
          if (queryReFetcher.isPresent()) {
            queryReFetcher.get().cancel();
          }
        } finally {
          tracker.unregisterCall(this);
          originalCallback.set(null);
        }
        break;
      case IDLE:
        state.set(CANCELED);
        break;
      case CANCELED:
      case TERMINATED:
        // These are not illegal states, but cancelling does nothing
        break;
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  @Override public boolean isCanceled() {
    return state.get() == CANCELED;
  }

  @Override @NotNull public RealApolloCall<D> clone() {
    return toBuilder().build();
  }

  @NotNull @Override public ApolloMutationCall<D> refetchQueries(@NotNull OperationName... operationNames) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .refetchQueryNames(Arrays.asList(checkNotNull(operationNames, "operationNames == null")))
        .build();
  }

  @NotNull @Override public ApolloMutationCall<D> refetchQueries(@NotNull Query... queries) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .refetchQueries(Arrays.asList(checkNotNull(queries, "queries == null")))
        .build();
  }

  @NotNull @Override public Operation<D, ?> operation() {
    return operation;
  }

  private ApolloInterceptor.CallBack interceptorCallbackProxy() {
    return new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@NotNull final ApolloInterceptor.InterceptorResponse response) {
        Optional<Callback<D>> callback = responseCallback();
        if (!callback.isPresent()) {
          logger.d("onResponse for operation: %s. No callback present.", operation().name().name());
          return;
        }
        //noinspection unchecked
        callback.get().onResponse(response.parsedResponse.get());
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        Optional<Callback<D>> callback = terminate();
        if (!callback.isPresent()) {
          logger.d(e, "onFailure for operation: %s. No callback present.", operation().name().name());
          return;
        }
        if (e instanceof ApolloHttpException) {
          callback.get().onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloParseException) {
          callback.get().onParseError((ApolloParseException) e);
        } else if (e instanceof ApolloNetworkException) {
          callback.get().onNetworkError((ApolloNetworkException) e);
        } else {
          callback.get().onFailure(e);
        }
      }

      @Override public void onCompleted() {
        Optional<Callback<D>> callback = terminate();
        if (queryReFetcher.isPresent()) {
          queryReFetcher.get().refetch();
        }
        if (!callback.isPresent()) {
          logger.d("onCompleted for operation: %s. No callback present.", operation().name().name());
          return;
        }
        callback.get().onStatusEvent(StatusEvent.COMPLETED);
      }

      @SuppressWarnings("ResultOfMethodCallIgnored")
      @Override public void onFetch(final ApolloInterceptor.FetchSourceType sourceType) {
        responseCallback().apply(new Action<Callback<D>>() {
          @Override public void apply(@NotNull Callback<D> callback) {
            switch (sourceType) {
              case CACHE:
                callback.onStatusEvent(StatusEvent.FETCH_CACHE);
                break;

              case NETWORK:
                callback.onStatusEvent(StatusEvent.FETCH_NETWORK);
                break;

              default:
                break;
            }
          }
        });
      }
    };
  }

  @NotNull public Builder<D> toBuilder() {
    return RealApolloCall.<D>builder()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(httpCachePolicy)
        .responseFieldMapperFactory(responseFieldMapperFactory)
        .scalarTypeAdapters(customScalarAdapters)
        .apolloStore(apolloStore)
        .cacheHeaders(cacheHeaders)
        .requestHeaders(requestHeaders)
        .responseFetcher(responseFetcher)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .applicationInterceptorFactories(applicationInterceptorFactories)
        .autoPersistedOperationsInterceptorFactory(autoPersistedOperationsInterceptorFactory)
        .tracker(tracker)
        .refetchQueryNames(refetchQueryNames)
        .refetchQueries(refetchQueries)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .useHttpGetMethodForQueries(useHttpGetMethodForQueries)
        .useHttpGetMethodForPersistedQueries(useHttpGetMethodForPersistedQueries)
        .optimisticUpdates(optimisticUpdates)
        .writeToNormalizedCacheAsynchronously(writeToNormalizedCacheAsynchronously);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private synchronized void activate(Optional<Callback<D>> callback) {
    switch (state.get()) {
      case IDLE:
        originalCallback.set(callback.orNull());
        tracker.registerCall(this);
        callback.apply(new Action<Callback<D>>() {
          @Override public void apply(@NotNull Callback<D> callback) {
            callback.onStatusEvent(StatusEvent.SCHEDULED);
          }
        });
        break;
      case CANCELED:
        throw new ApolloCanceledException();
      case TERMINATED:
      case ACTIVE:
        throw new IllegalStateException("Already Executed");
      default:
        throw new IllegalStateException("Unknown state");
    }
    state.set(ACTIVE);
  }

  synchronized Optional<Callback<D>> responseCallback() {
    switch (state.get()) {
      case ACTIVE:
      case CANCELED:
        return Optional.fromNullable(originalCallback.get());
      case IDLE:
      case TERMINATED:
        throw new IllegalStateException(
            CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  synchronized Optional<Callback<D>> terminate() {
    switch (state.get()) {
      case ACTIVE:
        tracker.unregisterCall(this);
        state.set(TERMINATED);
        return Optional.fromNullable(originalCallback.getAndSet(null));
      case CANCELED:
        return Optional.fromNullable(originalCallback.getAndSet(null));
      case IDLE:
      case TERMINATED:
        throw new IllegalStateException(
            CallState.IllegalStateMessage.forCurrentState(state.get()).expected(ACTIVE, CANCELED));
      default:
        throw new IllegalStateException("Unknown state");
    }
  }

  private ApolloInterceptorChain prepareInterceptorChain(Operation operation) {
    HttpCachePolicy.Policy httpCachePolicy = operation instanceof Query ? this.httpCachePolicy : null;
    ResponseFieldMapper responseFieldMapper = responseFieldMapperFactory.create(operation);

    List<ApolloInterceptor> interceptors = new ArrayList<>();

    for (ApolloInterceptorFactory factory : applicationInterceptorFactories) {
      ApolloInterceptor interceptor = factory.newInterceptor(logger, operation);
      if (interceptor != null) {
        interceptors.add(interceptor);
      }
    }
    interceptors.addAll(applicationInterceptors);

    interceptors.add(responseFetcher.provideInterceptor(logger));
    interceptors.add(new ApolloCacheInterceptor(
        apolloStore,
        responseFieldMapper,
        dispatcher,
        logger,
        writeToNormalizedCacheAsynchronously));
    if (autoPersistedOperationsInterceptorFactory != null) {
      ApolloInterceptor interceptor = autoPersistedOperationsInterceptorFactory.newInterceptor(logger, operation);
      if (interceptor != null) {
        interceptors.add(interceptor);
      }
    } else {
      if (enableAutoPersistedQueries && (operation instanceof Query || operation instanceof Mutation)) {
        interceptors.add(new ApolloAutoPersistedOperationInterceptor(
            logger,
            useHttpGetMethodForPersistedQueries && !(operation instanceof Mutation)));
      }
    }
    interceptors.add(new ApolloParseInterceptor(httpCache, apolloStore.networkResponseNormalizer(), responseFieldMapper,
        customScalarAdapters, logger));
    interceptors.add(new ApolloServerInterceptor(serverUrl, httpCallFactory, httpCachePolicy, false, customScalarAdapters,
        logger));

    return new RealApolloInterceptorChain(interceptors);
  }

  public static final class Builder<D extends Query.Data> implements ApolloQueryCall.Builder<D>, ApolloMutationCall.Builder<D> {
    Operation operation;
    HttpUrl serverUrl;
    Call.Factory httpCallFactory;
    HttpCache httpCache;
    HttpCachePolicy.Policy httpCachePolicy;
    ResponseFieldMapperFactory responseFieldMapperFactory;
    CustomScalarAdapters customScalarAdapters;
    ApolloStore apolloStore;
    ResponseFetcher responseFetcher;
    CacheHeaders cacheHeaders;
    RequestHeaders requestHeaders = RequestHeaders.NONE;
    Executor dispatcher;
    ApolloLogger logger;
    List<ApolloInterceptor> applicationInterceptors;
    List<ApolloInterceptorFactory> applicationInterceptorFactories;
    ApolloInterceptorFactory autoPersistedOperationsInterceptorFactory;
    List<OperationName> refetchQueryNames = emptyList();
    List<Query> refetchQueries = emptyList();
    ApolloCallTracker tracker;
    boolean enableAutoPersistedQueries;
    Optional<Operation.Data> optimisticUpdates = Optional.absent();
    boolean useHttpGetMethodForQueries;
    boolean useHttpGetMethodForPersistedQueries;
    boolean writeToNormalizedCacheAsynchronously;

    public Builder<D> operation(Operation operation) {
      this.operation = operation;
      return this;
    }

    public Builder<D> serverUrl(HttpUrl serverUrl) {
      this.serverUrl = serverUrl;
      return this;
    }

    public Builder<D> httpCallFactory(Call.Factory httpCallFactory) {
      this.httpCallFactory = httpCallFactory;
      return this;
    }

    public Builder<D> httpCache(HttpCache httpCache) {
      this.httpCache = httpCache;
      return this;
    }

    public Builder<D> responseFieldMapperFactory(ResponseFieldMapperFactory responseFieldMapperFactory) {
      this.responseFieldMapperFactory = responseFieldMapperFactory;
      return this;
    }

    public Builder<D> scalarTypeAdapters(CustomScalarAdapters customScalarAdapters) {
      this.customScalarAdapters = customScalarAdapters;
      return this;
    }

    public Builder<D> apolloStore(ApolloStore apolloStore) {
      this.apolloStore = apolloStore;
      return this;
    }

    @NotNull @Override public Builder<D> cacheHeaders(@NotNull CacheHeaders cacheHeaders) {
      this.cacheHeaders = cacheHeaders;
      return this;
    }

    @NotNull @Override public Builder<D> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy) {
      this.httpCachePolicy = httpCachePolicy;
      return this;
    }

    @NotNull @Override public Builder<D> responseFetcher(@NotNull ResponseFetcher responseFetcher) {
      this.responseFetcher = responseFetcher;
      return this;
    }

    @NotNull @Override public Builder<D> requestHeaders(@NotNull RequestHeaders requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    @NotNull @Override public Builder<D> refetchQueryNames(@NotNull List<OperationName> refetchQueryNames) {
      this.refetchQueryNames = new ArrayList<>(refetchQueryNames);
      return this;
    }

    @NotNull @Override public Builder<D> refetchQueries(@NotNull List<Query> refetchQueries) {
      this.refetchQueries = new ArrayList<>(refetchQueries);
      return this;
    }

    public Builder<D> dispatcher(Executor dispatcher) {
      this.dispatcher = dispatcher;
      return this;
    }

    public Builder<D> logger(ApolloLogger logger) {
      this.logger = logger;
      return this;
    }

    public Builder<D> tracker(ApolloCallTracker tracker) {
      this.tracker = tracker;
      return this;
    }

    public Builder<D> applicationInterceptors(List<ApolloInterceptor> applicationInterceptors) {
      this.applicationInterceptors = applicationInterceptors;
      return this;
    }

    public Builder<D> applicationInterceptorFactories(List<ApolloInterceptorFactory> applicationInterceptorFactories) {
      this.applicationInterceptorFactories = applicationInterceptorFactories;
      return this;
    }

    public Builder<D> autoPersistedOperationsInterceptorFactory(ApolloInterceptorFactory interceptorFactory) {
      this.autoPersistedOperationsInterceptorFactory = interceptorFactory;
      return this;
    }

    public Builder<D> enableAutoPersistedQueries(boolean enableAutoPersistedQueries) {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries;
      return this;
    }

    public Builder<D> optimisticUpdates(Optional<Operation.Data> optimisticUpdates) {
      this.optimisticUpdates = optimisticUpdates;
      return this;
    }

    public Builder<D> useHttpGetMethodForQueries(boolean useHttpGetMethodForQueries) {
      this.useHttpGetMethodForQueries = useHttpGetMethodForQueries;
      return this;
    }

    public Builder<D> useHttpGetMethodForPersistedQueries(boolean useHttpGetMethodForPersistedQueries) {
      this.useHttpGetMethodForPersistedQueries = useHttpGetMethodForPersistedQueries;
      return this;
    }

    public Builder<D> writeToNormalizedCacheAsynchronously(boolean writeToNormalizedCacheAsynchronously) {
      this.writeToNormalizedCacheAsynchronously = writeToNormalizedCacheAsynchronously;
      return this;
    }

    Builder() {
    }

    @NotNull public RealApolloCall<D> build() {
      return new RealApolloCall<>(this);
    }
  }
}
