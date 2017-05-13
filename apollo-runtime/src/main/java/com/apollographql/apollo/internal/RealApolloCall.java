package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloMutationCall;
import com.apollographql.apollo.ApolloQueryCall;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.interceptor.ApolloCacheInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloParseInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.apollographql.apollo.internal.util.ApolloLogger;
import com.squareup.moshi.Moshi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess")
public final class RealApolloCall<T> implements ApolloQueryCall<T>, ApolloMutationCall<T> {
  final Operation operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final HttpCache httpCache;
  final HttpCachePolicy.Policy httpCachePolicy;
  final Moshi moshi;
  final ResponseFieldMapper responseFieldMapper;
  final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  final ApolloStore apolloStore;
  final CacheControl cacheControl;
  final CacheHeaders cacheHeaders;
  final ApolloInterceptorChain interceptorChain;
  final ExecutorService dispatcher;
  final ApolloLogger logger;
  final List<ApolloInterceptor> applicationInterceptors;
  final List<OperationName> refetchQueryNames;
  final AtomicBoolean executed = new AtomicBoolean();
  volatile boolean canceled;

  public RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      HttpCachePolicy.Policy httpCachePolicy, Moshi moshi, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, ApolloStore apolloStore, CacheControl cacheControl,
      CacheHeaders cacheHeaders, ExecutorService dispatcher, ApolloLogger logger,
      List<ApolloInterceptor> applicationInterceptors, List<OperationName> refetchQueryNames) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.httpCache = httpCache;
    this.httpCachePolicy = httpCachePolicy;
    this.moshi = moshi;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.apolloStore = apolloStore;
    this.cacheControl = cacheControl;
    this.cacheHeaders = cacheHeaders;
    this.dispatcher = dispatcher;
    this.logger = logger;
    this.applicationInterceptors = applicationInterceptors;
    this.refetchQueryNames = refetchQueryNames;
    interceptorChain = prepareInterceptorChain(operation);
  }

  @SuppressWarnings("unchecked") @Nonnull @Override public Response<T> execute() throws ApolloException {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }

    if (canceled) {
      throw new ApolloCanceledException("Canceled");
    }

    Response<T> response;
    try {
      response = interceptorChain.proceed().parsedResponse.or(new Response(operation));
    } catch (Exception e) {
      if (canceled) {
        throw new ApolloCanceledException("Canceled", e);
      } else {
        throw e;
      }
    }

    if (canceled) {
      throw new ApolloCanceledException("Canceled");
    }

    return response;
  }

  @Override public void enqueue(@Nullable final Callback<T> callback) {
    if (!executed.compareAndSet(false, true)) {
      throw new IllegalStateException("Already Executed");
    }

    interceptorChain.proceedAsync(dispatcher, new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        if (callback == null) {
          return;
        }

        if (canceled) {
          callback.onCanceledError(new ApolloCanceledException("Canceled"));
        } else {
          //noinspection unchecked
          callback.onResponse(response.parsedResponse.get());
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        if (callback == null) {
          return;
        }

        if (canceled) {
          callback.onCanceledError(new ApolloCanceledException("Canceled", e));
        } else if (e instanceof ApolloHttpException) {
          callback.onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloParseException) {
          callback.onParseError((ApolloParseException) e);
        } else if (e instanceof ApolloNetworkException) {
          callback.onNetworkError((ApolloNetworkException) e);
        } else {
          callback.onFailure(e);
        }
      }
    });
  }

  @Nonnull @Override public RealApolloQueryWatcher<T> watcher() {
    return new RealApolloQueryWatcher<>(clone(), apolloStore);
  }

  @Nonnull @Override public RealApolloCall<T> httpCachePolicy(@Nonnull HttpCachePolicy.Policy httpCachePolicy) {
    if (executed.get()) throw new IllegalStateException("Already Executed");
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache,
        checkNotNull(httpCachePolicy, "httpCachePolicy == null"), moshi, responseFieldMapper, customTypeAdapters,
        apolloStore, cacheControl, cacheHeaders, dispatcher, logger, applicationInterceptors, refetchQueryNames);
  }

  @Nonnull @Override public RealApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl) {
    if (executed.get()) throw new IllegalStateException("Already Executed");
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCachePolicy, moshi,
        responseFieldMapper, customTypeAdapters, apolloStore, checkNotNull(cacheControl, "cacheControl == null"),
        cacheHeaders, dispatcher, logger, applicationInterceptors, refetchQueryNames);
  }

  @Nonnull @Override public RealApolloCall<T> cacheHeaders(@Nonnull CacheHeaders cacheHeaders) {
    if (executed.get()) throw new IllegalStateException("Already Executed");
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCachePolicy, moshi,
        responseFieldMapper, customTypeAdapters, apolloStore, cacheControl,
        checkNotNull(cacheHeaders, "cacheHeaders == null"), dispatcher, logger, applicationInterceptors,
        refetchQueryNames);
  }

  @Override public void cancel() {
    canceled = true;
    interceptorChain.dispose();
  }

  @Override public boolean isCanceled() {
    return canceled;
  }

  @Override @Nonnull public RealApolloCall<T> clone() {
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCachePolicy, moshi,
        responseFieldMapper, customTypeAdapters, apolloStore, cacheControl, cacheHeaders, dispatcher, logger,
        applicationInterceptors, refetchQueryNames);
  }

  @Nonnull @Override public ApolloMutationCall<T> refetchQueries(@Nonnull OperationName... operationNames) {
    if (executed.get()) throw new IllegalStateException("Already Executed");
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCachePolicy, moshi,
        responseFieldMapper, customTypeAdapters, apolloStore, cacheControl, cacheHeaders, dispatcher, logger,
        applicationInterceptors, Arrays.asList(checkNotNull(operationNames, "operationNames == null")));
  }

  private ApolloInterceptorChain prepareInterceptorChain(Operation operation) {
    List<ApolloInterceptor> interceptors = new ArrayList<>();
    HttpCachePolicy.Policy httpCachePolicy = operation instanceof Query ? this.httpCachePolicy : null;

    interceptors.addAll(applicationInterceptors);
    interceptors.add(new ApolloCacheInterceptor(apolloStore, cacheControl, cacheHeaders, responseFieldMapper,
        customTypeAdapters, dispatcher, logger));
    interceptors.add(new ApolloParseInterceptor(httpCache, apolloStore.networkResponseNormalizer(), responseFieldMapper,
        customTypeAdapters, logger));
    interceptors.add(new ApolloServerInterceptor(serverUrl, httpCallFactory, httpCachePolicy, false, moshi, logger));

    return new RealApolloInterceptorChain(operation, interceptors);
  }
}
