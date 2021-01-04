package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloQueryWatcher;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorFactory;
import okhttp3.Call;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class QueryReFetcher {
  final ApolloLogger logger;
  private final List<RealApolloCall> calls;
  private List<OperationName> queryWatchers;
  private ApolloCallTracker callTracker;
  private final AtomicBoolean executed = new AtomicBoolean();
  OnCompleteCallback onCompleteCallback;

  static Builder builder() {
    return new Builder();
  }

  QueryReFetcher(Builder builder) {
    logger = builder.logger;
    calls = new ArrayList<>(builder.queries.size());
    for (Query query : builder.queries) {
      calls.add(RealApolloCall.builder()
          .operation(query)
          .serverUrl(builder.serverUrl)
          .httpCallFactory(builder.httpCallFactory)
          .responseFieldMapperFactory(builder.responseFieldMapperFactory)
          .scalarTypeAdapters(builder.customScalarAdapters)
          .apolloStore(builder.apolloStore)
          .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
          .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
          .cacheHeaders(CacheHeaders.NONE)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .applicationInterceptorFactories(builder.applicationInterceptorFactories)
          .autoPersistedOperationsInterceptorFactory(builder.autoPersistedOperationsInterceptorFactory)
          .tracker(builder.callTracker)
          .dispatcher(builder.dispatcher)
          .build());
    }
    queryWatchers = builder.queryWatchers;
    callTracker = builder.callTracker;
  }

  void refetch() {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }

    refetchQueryWatchers();
    refetchQueries();
  }

  void cancel() {
    for (RealApolloCall call : calls) {
      call.cancel();
    }
  }

  private void refetchQueryWatchers() {
    try {
      for (OperationName operationName : queryWatchers) {
        for (ApolloQueryWatcher queryWatcher : callTracker.activeQueryWatchers(operationName)) {
          queryWatcher.refetch();
        }
      }
    } catch (Exception e) {
      logger.e(e, "Failed to re-fetch query watcher");
    }
  }

  private void refetchQueries() {
    final OnCompleteCallback completeCallback = onCompleteCallback;
    final AtomicInteger callsLeft = new AtomicInteger(calls.size());
    for (final RealApolloCall call : calls) {
      //noinspection unchecked
      call.enqueue(new ApolloCall.Callback() {
        @Override public void onResponse(@NotNull Response response) {
          if (callsLeft.decrementAndGet() == 0 && completeCallback != null) {
            completeCallback.onFetchComplete();
          }
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          if (logger != null) {
            logger.e(e, "Failed to fetch query: %s", call.operation);
          }

          if (callsLeft.decrementAndGet() == 0 && completeCallback != null) {
            completeCallback.onFetchComplete();
          }
        }
      });
    }
  }

  static final class Builder {
    List<Query> queries = Collections.emptyList();
    List<OperationName> queryWatchers = Collections.emptyList();
    HttpUrl serverUrl;
    Call.Factory httpCallFactory;
    ResponseFieldMapperFactory responseFieldMapperFactory;
    CustomScalarAdapters customScalarAdapters;
    ApolloStore apolloStore;
    Executor dispatcher;
    ApolloLogger logger;
    List<ApolloInterceptor> applicationInterceptors;
    List<ApolloInterceptorFactory> applicationInterceptorFactories;
    ApolloInterceptorFactory autoPersistedOperationsInterceptorFactory;
    ApolloCallTracker callTracker;

    Builder queries(List<Query> queries) {
      this.queries = queries != null ? queries : Collections.<Query>emptyList();
      return this;
    }

    public Builder queryWatchers(List<OperationName> queryWatchers) {
      this.queryWatchers = queryWatchers != null ? queryWatchers : Collections.<OperationName>emptyList();
      return this;
    }

    Builder serverUrl(HttpUrl serverUrl) {
      this.serverUrl = serverUrl;
      return this;
    }

    Builder httpCallFactory(Call.Factory httpCallFactory) {
      this.httpCallFactory = httpCallFactory;
      return this;
    }

    Builder responseFieldMapperFactory(ResponseFieldMapperFactory responseFieldMapperFactory) {
      this.responseFieldMapperFactory = responseFieldMapperFactory;
      return this;
    }

    Builder scalarTypeAdapters(CustomScalarAdapters customScalarAdapters) {
      this.customScalarAdapters = customScalarAdapters;
      return this;
    }

    Builder apolloStore(ApolloStore apolloStore) {
      this.apolloStore = apolloStore;
      return this;
    }

    Builder dispatcher(Executor dispatcher) {
      this.dispatcher = dispatcher;
      return this;
    }

    Builder logger(ApolloLogger logger) {
      this.logger = logger;
      return this;
    }

    Builder applicationInterceptors(List<ApolloInterceptor> applicationInterceptors) {
      this.applicationInterceptors = applicationInterceptors;
      return this;
    }

    Builder applicationInterceptorFactories(List<ApolloInterceptorFactory> applicationInterceptorFactories) {
      this.applicationInterceptorFactories = applicationInterceptorFactories;
      return this;
    }

    Builder autoPersistedOperationsInterceptorFactory(ApolloInterceptorFactory interceptorFactories) {
      this.autoPersistedOperationsInterceptorFactory = interceptorFactories;
      return this;
    }

    Builder callTracker(ApolloCallTracker callTracker) {
      this.callTracker = callTracker;
      return this;
    }

    QueryReFetcher build() {
      return new QueryReFetcher(this);
    }

    Builder() {
    }
  }

  interface OnCompleteCallback {
    void onFetchComplete();
  }
}
