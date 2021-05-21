package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloMutationCall;
import com.apollographql.apollo.ApolloQueryCall;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarTypeAdapters;
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
import com.apollographql.apollo.internal.batch.BatchPoller;
import com.apollographql.apollo.internal.interceptor.ApolloBatchingInterceptor;
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
public final class RealApolloCall<T> implements ApolloQueryCall<T>, ApolloMutationCall<T> {
  final Operation operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final HttpCache httpCache;
  final HttpCachePolicy.Policy httpCachePolicy;
  final ScalarTypeAdapters scalarTypeAdapters;
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
  final AtomicReference<Callback<T>> originalCallback = new AtomicReference<>();
  final Optional<Operation.Data> optimisticUpdates;
  final boolean useHttpGetMethodForQueries;
  final boolean useHttpGetMethodForPersistedQueries;
  final boolean writeToNormalizedCacheAsynchronously;
  final BatchPoller batchPoller;

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  RealApolloCall(Builder<T> builder) {
    operation = builder.operation;
    serverUrl = builder.serverUrl;
    httpCallFactory = builder.httpCallFactory;
    httpCache = builder.httpCache;
    httpCachePolicy = builder.httpCachePolicy;
    scalarTypeAdapters = builder.scalarTypeAdapters;
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
          .scalarTypeAdapters(builder.scalarTypeAdapters)
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
    batchPoller = builder.batchPoller;
    interceptorChain = prepareInterceptorChain(operation);
  }

  @Override public void enqueue(@Nullable final Callback<T> responseCallback) {
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

  @NotNull @Override public RealApolloQueryWatcher<T> watcher() {
    return new RealApolloQueryWatcher<>(clone(), apolloStore, logger, tracker, ApolloResponseFetchers.CACHE_FIRST);
  }

  @NotNull @Override public RealApolloCall<T> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .httpCachePolicy(checkNotNull(httpCachePolicy, "httpCachePolicy == null"))
        .build();
  }

  @NotNull @Override public RealApolloCall<T> responseFetcher(@NotNull ResponseFetcher fetcher) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .responseFetcher(checkNotNull(fetcher, "responseFetcher == null"))
        .build();
  }

  @NotNull @Override public RealApolloCall<T> cacheHeaders(@NotNull CacheHeaders cacheHeaders) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .cacheHeaders(checkNotNull(cacheHeaders, "cacheHeaders == null"))
        .build();
  }

  @NotNull @Override public RealApolloCall<T> requestHeaders(@NotNull RequestHeaders requestHeaders) {
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

  @Override @NotNull public RealApolloCall<T> clone() {
    return toBuilder().build();
  }

  @NotNull @Override public ApolloMutationCall<T> refetchQueries(@NotNull OperationName... operationNames) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .refetchQueryNames(Arrays.asList(checkNotNull(operationNames, "operationNames == null")))
        .build();
  }

  @NotNull @Override public ApolloMutationCall<T> refetchQueries(@NotNull Query... queries) {
    if (state.get() != IDLE) throw new IllegalStateException("Already Executed");
    return toBuilder()
        .refetchQueries(Arrays.asList(checkNotNull(queries, "queries == null")))
        .build();
  }

  @NotNull @Override public Operation operation() {
    return operation;
  }

  private ApolloInterceptor.CallBack interceptorCallbackProxy() {
    return new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@NotNull final ApolloInterceptor.InterceptorResponse response) {
        Optional<Callback<T>> callback = responseCallback();
        if (!callback.isPresent()) {
          logger.d("onResponse for operation: %s. No callback present.", operation().name().name());
          return;
        }
        //noinspection unchecked
        callback.get().onResponse(response.parsedResponse.get());
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        Optional<Callback<T>> callback = terminate();
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
        Optional<Callback<T>> callback = terminate();
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
        responseCallback().apply(new Action<Callback<T>>() {
          @Override public void apply(@NotNull Callback<T> callback) {
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

  @NotNull public Builder<T> toBuilder() {
    return RealApolloCall.<T>builder()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(httpCachePolicy)
        .scalarTypeAdapters(scalarTypeAdapters)
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
        .writeToNormalizedCacheAsynchronously(writeToNormalizedCacheAsynchronously)
        .batchPoller(batchPoller);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private synchronized void activate(Optional<Callback<T>> callback) {
    switch (state.get()) {
      case IDLE:
        originalCallback.set(callback.orNull());
        tracker.registerCall(this);
        callback.apply(new Action<Callback<T>>() {
          @Override public void apply(@NotNull Callback<T> callback) {
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

  synchronized Optional<Callback<T>> responseCallback() {
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

  synchronized Optional<Callback<T>> terminate() {
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
    ResponseFieldMapper responseFieldMapper = operation.responseFieldMapper();

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
        scalarTypeAdapters, logger));

    if (batchPoller != null) {
      if (useHttpGetMethodForQueries || useHttpGetMethodForPersistedQueries) {
        throw new ApolloException("Batching is not supported when using HTTP Get method queries");
      }
      interceptors.add(new ApolloBatchingInterceptor(batchPoller));
    } else {
      interceptors.add(new ApolloServerInterceptor(serverUrl, httpCallFactory, httpCachePolicy, false,
          scalarTypeAdapters, logger));
    }

    return new RealApolloInterceptorChain(interceptors);
  }

  public static final class Builder<T> implements ApolloQueryCall.Builder<T>, ApolloMutationCall.Builder<T> {
    Operation operation;
    HttpUrl serverUrl;
    Call.Factory httpCallFactory;
    HttpCache httpCache;
    HttpCachePolicy.Policy httpCachePolicy;
    ScalarTypeAdapters scalarTypeAdapters;
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
    BatchPoller batchPoller;

    public Builder<T> operation(Operation operation) {
      this.operation = operation;
      return this;
    }

    public Builder<T> serverUrl(HttpUrl serverUrl) {
      this.serverUrl = serverUrl;
      return this;
    }

    public Builder<T> httpCallFactory(Call.Factory httpCallFactory) {
      this.httpCallFactory = httpCallFactory;
      return this;
    }

    public Builder<T> httpCache(HttpCache httpCache) {
      this.httpCache = httpCache;
      return this;
    }

    /**
     * @deprecated The mapper factory is no longer used and will be removed in the future.
     */
    @Deprecated
    public Builder<T> responseFieldMapperFactory(@SuppressWarnings("unused") ResponseFieldMapperFactory responseFieldMapperFactory) {
      return this;
    }

    public Builder<T> scalarTypeAdapters(ScalarTypeAdapters scalarTypeAdapters) {
      this.scalarTypeAdapters = scalarTypeAdapters;
      return this;
    }

    public Builder<T> apolloStore(ApolloStore apolloStore) {
      this.apolloStore = apolloStore;
      return this;
    }

    @NotNull @Override public Builder<T> cacheHeaders(@NotNull CacheHeaders cacheHeaders) {
      this.cacheHeaders = cacheHeaders;
      return this;
    }

    @NotNull @Override public Builder<T> httpCachePolicy(@NotNull HttpCachePolicy.Policy httpCachePolicy) {
      this.httpCachePolicy = httpCachePolicy;
      return this;
    }

    @NotNull @Override public Builder<T> responseFetcher(@NotNull ResponseFetcher responseFetcher) {
      this.responseFetcher = responseFetcher;
      return this;
    }

    @NotNull @Override public Builder<T> requestHeaders(@NotNull RequestHeaders requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    @NotNull @Override public Builder<T> refetchQueryNames(@NotNull List<OperationName> refetchQueryNames) {
      this.refetchQueryNames = new ArrayList<>(refetchQueryNames);
      return this;
    }

    @NotNull @Override public Builder<T> refetchQueries(@NotNull List<Query> refetchQueries) {
      this.refetchQueries = new ArrayList<>(refetchQueries);
      return this;
    }

    public Builder<T> dispatcher(Executor dispatcher) {
      this.dispatcher = dispatcher;
      return this;
    }

    public Builder<T> logger(ApolloLogger logger) {
      this.logger = logger;
      return this;
    }

    public Builder<T> tracker(ApolloCallTracker tracker) {
      this.tracker = tracker;
      return this;
    }

    public Builder<T> applicationInterceptors(List<ApolloInterceptor> applicationInterceptors) {
      this.applicationInterceptors = applicationInterceptors;
      return this;
    }

    public Builder<T> applicationInterceptorFactories(List<ApolloInterceptorFactory> applicationInterceptorFactories) {
      this.applicationInterceptorFactories = applicationInterceptorFactories;
      return this;
    }

    public Builder<T> autoPersistedOperationsInterceptorFactory(ApolloInterceptorFactory interceptorFactory) {
      this.autoPersistedOperationsInterceptorFactory = interceptorFactory;
      return this;
    }

    public Builder<T> enableAutoPersistedQueries(boolean enableAutoPersistedQueries) {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries;
      return this;
    }

    public Builder<T> optimisticUpdates(Optional<Operation.Data> optimisticUpdates) {
      this.optimisticUpdates = optimisticUpdates;
      return this;
    }

    public Builder<T> useHttpGetMethodForQueries(boolean useHttpGetMethodForQueries) {
      this.useHttpGetMethodForQueries = useHttpGetMethodForQueries;
      return this;
    }

    public Builder<T> useHttpGetMethodForPersistedQueries(boolean useHttpGetMethodForPersistedQueries) {
      this.useHttpGetMethodForPersistedQueries = useHttpGetMethodForPersistedQueries;
      return this;
    }

    public Builder<T> writeToNormalizedCacheAsynchronously(boolean writeToNormalizedCacheAsynchronously) {
      this.writeToNormalizedCacheAsynchronously = writeToNormalizedCacheAsynchronously;
      return this;
    }

    public Builder<T> batchPoller(BatchPoller batchPoller) {
      this.batchPoller = batchPoller;
      return this;
    }

    Builder() {
    }

    @NotNull public RealApolloCall<T> build() {
      return new RealApolloCall<>(this);
    }
  }
}
