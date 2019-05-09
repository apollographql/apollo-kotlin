package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloMutationCall;
import com.apollographql.apollo.ApolloQueryCall;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.Action;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.interceptor.ApolloAutoPersistedQueryInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloCacheInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloParseInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.apollographql.apollo.request.RequestHeaders;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

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
  final ResponseFieldMapperFactory responseFieldMapperFactory;
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
  final List<OperationName> refetchQueryNames;
  final List<Query> refetchQueries;
  final Optional<QueryReFetcher> queryReFetcher;
  final boolean enableAutoPersistedQueries;
  final AtomicReference<CallState> state = new AtomicReference<>(IDLE);
  final AtomicReference<Callback<T>> originalCallback = new AtomicReference<>();
  final Optional<Operation.Data> optimisticUpdates;
  final boolean useHttpGetMethodForQueries;

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  RealApolloCall(Builder<T> builder) {
    operation = builder.operation;
    serverUrl = builder.serverUrl;
    httpCallFactory = builder.httpCallFactory;
    httpCache = builder.httpCache;
    httpCachePolicy = builder.httpCachePolicy;
    responseFieldMapperFactory = builder.responseFieldMapperFactory;
    scalarTypeAdapters = builder.scalarTypeAdapters;
    apolloStore = builder.apolloStore;
    responseFetcher = builder.responseFetcher;
    cacheHeaders = builder.cacheHeaders;
    requestHeaders = builder.requestHeaders;
    dispatcher = builder.dispatcher;
    logger = builder.logger;
    applicationInterceptors = builder.applicationInterceptors;
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
          .scalarTypeAdapters(builder.scalarTypeAdapters)
          .apolloStore(builder.apolloStore)
          .dispatcher(builder.dispatcher)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .callTracker(builder.tracker)
          .build());
    }
    enableAutoPersistedQueries = builder.enableAutoPersistedQueries;
    interceptorChain = prepareInterceptorChain(operation);
    optimisticUpdates = builder.optimisticUpdates;
    useHttpGetMethodForQueries = builder.useHttpGetMethodForQueries;
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
        .build();
    interceptorChain.proceedAsync(request, dispatcher, interceptorCallbackProxy());
  }

  @NotNull @Override public RealApolloQueryWatcher<T> watcher() {
    return new RealApolloQueryWatcher<>(clone(), apolloStore, logger, tracker);
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

  public Builder<T> toBuilder() {
    return RealApolloCall.<T>builder()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(httpCachePolicy)
        .responseFieldMapperFactory(responseFieldMapperFactory)
        .scalarTypeAdapters(scalarTypeAdapters)
        .apolloStore(apolloStore)
        .cacheHeaders(cacheHeaders)
        .requestHeaders(requestHeaders)
        .responseFetcher(responseFetcher)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .tracker(tracker)
        .refetchQueryNames(refetchQueryNames)
        .refetchQueries(refetchQueries)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .optimisticUpdates(optimisticUpdates);
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
        throw new ApolloCanceledException("Call is cancelled.");
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
    List<ApolloInterceptor> interceptors = new ArrayList<>();
    HttpCachePolicy.Policy httpCachePolicy = operation instanceof Query ? this.httpCachePolicy : null;
    ResponseFieldMapper responseFieldMapper = responseFieldMapperFactory.create(operation);

    interceptors.addAll(applicationInterceptors);
    interceptors.add(responseFetcher.provideInterceptor(logger));
    interceptors.add(new ApolloCacheInterceptor(apolloStore, responseFieldMapper, dispatcher, logger));
    if (operation instanceof Query && enableAutoPersistedQueries) {
      interceptors.add(new ApolloAutoPersistedQueryInterceptor(logger));
    }
    interceptors.add(new ApolloParseInterceptor(httpCache, apolloStore.networkResponseNormalizer(), responseFieldMapper,
        scalarTypeAdapters, logger));
    interceptors.add(new ApolloServerInterceptor(serverUrl, httpCallFactory, httpCachePolicy, false, scalarTypeAdapters,
        logger, useHttpGetMethodForQueries));

    return new RealApolloInterceptorChain(interceptors);
  }

  public static final class Builder<T> {
    Operation operation;
    HttpUrl serverUrl;
    Call.Factory httpCallFactory;
    HttpCache httpCache;
    HttpCachePolicy.Policy httpCachePolicy;
    ResponseFieldMapperFactory responseFieldMapperFactory;
    ScalarTypeAdapters scalarTypeAdapters;
    ApolloStore apolloStore;
    ResponseFetcher responseFetcher;
    CacheHeaders cacheHeaders;
    RequestHeaders requestHeaders = RequestHeaders.NONE;
    ApolloInterceptorChain interceptorChain;
    Executor dispatcher;
    ApolloLogger logger;
    List<ApolloInterceptor> applicationInterceptors;
    List<OperationName> refetchQueryNames = emptyList();
    List<Query> refetchQueries = emptyList();
    ApolloCallTracker tracker;
    boolean enableAutoPersistedQueries;
    Optional<Operation.Data> optimisticUpdates = Optional.absent();
    boolean useHttpGetMethodForQueries;

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

    public Builder<T> httpCachePolicy(HttpCachePolicy.Policy httpCachePolicy) {
      this.httpCachePolicy = httpCachePolicy;
      return this;
    }

    public Builder<T> responseFieldMapperFactory(ResponseFieldMapperFactory responseFieldMapperFactory) {
      this.responseFieldMapperFactory = responseFieldMapperFactory;
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

    public Builder<T> responseFetcher(ResponseFetcher responseFetcher) {
      this.responseFetcher = responseFetcher;
      return this;
    }

    public Builder<T> cacheHeaders(CacheHeaders cacheHeaders) {
      this.cacheHeaders = cacheHeaders;
      return this;
    }

    public Builder<T> requestHeaders(RequestHeaders requestHeaders) {
      this.requestHeaders = requestHeaders;
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

    public Builder<T> refetchQueryNames(List<OperationName> refetchQueryNames) {
      this.refetchQueryNames = refetchQueryNames != null ? new ArrayList<>(refetchQueryNames)
          : Collections.<OperationName>emptyList();
      return this;
    }

    public Builder<T> refetchQueries(List<Query> refetchQueries) {
      this.refetchQueries = refetchQueries != null ? new ArrayList<>(refetchQueries) : Collections.<Query>emptyList();
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

    Builder() {
    }

    public RealApolloCall<T> build() {
      return new RealApolloCall<>(this);
    }
  }
}
