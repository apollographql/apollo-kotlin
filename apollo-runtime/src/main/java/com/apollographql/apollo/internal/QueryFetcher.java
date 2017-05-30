package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.internal.util.ApolloLogger;
import com.squareup.moshi.Moshi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import okhttp3.Call;
import okhttp3.HttpUrl;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

final class QueryFetcher {
  private final ApolloLogger logger;
  private final List<RealApolloCall> calls;
  private final AtomicBoolean executed = new AtomicBoolean();
  private volatile boolean canceled;

  static Builder builder() {
    return new Builder();
  }

  QueryFetcher(Builder builder) {
    logger = builder.logger;
    calls = new ArrayList<>(builder.queries.size());
    for (Query query : builder.queries) {
      calls.add(RealApolloCall.builder()
          .operation(query)
          .serverUrl(builder.serverUrl)
          .httpCallFactory(builder.httpCallFactory)
          .moshi(builder.moshi)
          .responseFieldMapperFactory(builder.responseFieldMapperFactory)
          .customTypeAdapters(builder.customTypeAdapters)
          .apolloStore(builder.apolloStore)
          .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
          .cacheControl(CacheControl.NETWORK_ONLY)
          .cacheHeaders(CacheHeaders.NONE)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .tracker(builder.tracker)
          .dispatcher(builder.dispatcher)
          .build());
    }
  }

  void refetchSync() {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }

    for (RealApolloCall call : calls) {
      try {
        call.execute();
        if (canceled) {
          return;
        }
      } catch (Exception e) {
        if (logger != null) {
          logger.e(e, "Failed to fetch query: %s", call.operation);
        }
      }
    }
  }

  void refetchAsync(@Nonnull OnFetchCompleteCallback callback) {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }
    refetchAsync(0, checkNotNull(callback, "callback == null"));
  }

  void cancel() {
    canceled = true;
  }

  @SuppressWarnings("unchecked") private void refetchAsync(final int nextCallIndex,
      final OnFetchCompleteCallback callback) {
    final RealApolloCall call = nextCallIndex < calls.size() ? calls.get(nextCallIndex) : null;
    if (call == null) {
      callback.onFetchComplete();
      return;
    }

    if (canceled) {
      return;
    }

    call.enqueue(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        refetchAsync(nextCallIndex + 1, callback);
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        if (logger != null) {
          logger.e(e, "Failed to fetch query: %s", call.operation);
        }
        refetchAsync(nextCallIndex + 1, callback);
      }
    });
  }

  static final class Builder {
    List<Query> queries;
    HttpUrl serverUrl;
    Call.Factory httpCallFactory;
    Moshi moshi;
    ResponseFieldMapperFactory responseFieldMapperFactory;
    Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
    ApolloStore apolloStore;
    ExecutorService dispatcher;
    ApolloLogger logger;
    List<ApolloInterceptor> applicationInterceptors;
    ApolloCallTracker tracker;

    Builder queries(List<Query> queries) {
      this.queries = queries;
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

    Builder moshi(Moshi moshi) {
      this.moshi = moshi;
      return this;
    }

    Builder responseFieldMapperFactory(ResponseFieldMapperFactory responseFieldMapperFactory) {
      this.responseFieldMapperFactory = responseFieldMapperFactory;
      return this;
    }

    Builder customTypeAdapters(Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
      this.customTypeAdapters = customTypeAdapters;
      return this;
    }

    Builder apolloStore(ApolloStore apolloStore) {
      this.apolloStore = apolloStore;
      return this;
    }

    Builder dispatcher(ExecutorService dispatcher) {
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

    Builder tracker(ApolloCallTracker tracker) {
      this.tracker = tracker;
      return this;
    }

    QueryFetcher build() {
      return new QueryFetcher(this);
    }

    private Builder() {
    }
  }

  interface OnFetchCompleteCallback {
    void onFetchComplete();
  }
}
