package com.apollographql.android.impl;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.apollographql.android.cache.normalized.Cache;
import com.apollographql.android.cache.normalized.CacheControl;
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.cache.normalized.ReadableCache;
import com.apollographql.android.cache.normalized.Record;
import com.apollographql.android.cache.normalized.ResponseNormalizer;
import com.apollographql.android.cache.normalized.Transaction;
import com.apollographql.android.cache.normalized.WriteableCache;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ApolloCacheInterceptor implements ApolloInterceptor {
  private final Cache cache;
  private final CacheControl cacheControl;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final ExecutorService dispatcher;

  ApolloCacheInterceptor(Cache cache, CacheControl cacheControl, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, ExecutorService dispatcher) {
    this.cache = cache;
    this.cacheControl = cacheControl;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.dispatcher = dispatcher;
  }

  @Nonnull @Override public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain)
      throws IOException {
    InterceptorResponse cachedResponse = resolveCacheFirstResponse(operation);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    InterceptorResponse networkResponse = null;
    try {
      networkResponse = chain.proceed();
      return handleNetworkResponse(operation, networkResponse);
    } catch (Exception e) {
      InterceptorResponse response = resolveNetworkFirstCacheResponse(operation, networkResponse);
      if (response != null) {
        return response;
      }
      throw e;
    }
  }

  @Override
  public void interceptAsync(@Nonnull final Operation operation, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull final ExecutorService dispatcher, @Nonnull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        InterceptorResponse cachedResponse = resolveCacheFirstResponse(operation);
        if (cachedResponse != null) {
          callBack.onResponse(cachedResponse);
          return;
        }

        chain.proceedAsync(dispatcher, new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {
            try {
              callBack.onResponse(handleNetworkResponse(operation, response));
            } catch (Exception e) {
              callBack.onFailure(e);
            }
          }

          @Override public void onFailure(@Nonnull Throwable t) {
            InterceptorResponse response = resolveNetworkFirstCacheResponse(operation, null);
            if (response != null) {
              callBack.onResponse(response);
            } else {
              callBack.onFailure(t);
            }
          }
        });
      }
    });
  }

  @Override public void dispose() {
    //no op
  }

  private InterceptorResponse resolveCacheFirstResponse(Operation operation) {
    if (cacheControl == CacheControl.CACHE_ONLY || cacheControl == CacheControl.CACHE_FIRST) {
      ResponseNormalizer<Record> responseNormalizer = cache.cacheResponseNormalizer();
      Response cachedResponse = cachedResponse(operation, responseNormalizer);
      if (cacheControl == CacheControl.CACHE_ONLY
          || (cachedResponse != null && cachedResponse.data() != null && cachedResponse.isSuccessful())) {
        return new InterceptorResponse(null, cachedResponse, responseNormalizer.records());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked") private Response cachedResponse(final Operation operation,
      final ResponseNormalizer<Record> cacheResponseNormalizer) {
    return cache.readTransaction(new Transaction<ReadableCache, Response>() {
      @Nullable @Override public Response execute(ReadableCache cache) {
        cacheResponseNormalizer.willResolveRootQuery(operation);
        Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).key());
        if (rootRecord == null) {
          return new Response(operation);
        }
        try {
          RealResponseReader<Record> responseReader = new RealResponseReader<>(operation, rootRecord,
              new CacheFieldValueResolver(cache, operation.variables()), customTypeAdapters, cacheResponseNormalizer);
          return new Response(operation, responseFieldMapper.map(responseReader), null,
              cacheResponseNormalizer.dependentKeys());
        } catch (Exception e) {
          //TODO log me
          return new Response(operation);
        }
      }
    });
  }

  private InterceptorResponse handleNetworkResponse(Operation operation, InterceptorResponse networkResponse) {
    try {
      if (!networkResponse.httpResponse.get().isSuccessful()) {
        ResponseNormalizer<Record> responseNormalizer = cache.cacheResponseNormalizer();
        Response cachedResponse = cachedResponse(operation, responseNormalizer);
        if (cachedResponse != null && cachedResponse.data() != null && cachedResponse.isSuccessful()) {
          return new InterceptorResponse(networkResponse.httpResponse.get(), cachedResponse,
              responseNormalizer.records());
        }
      }

      final Collection<Record> records = networkResponse.cacheRecords.orNull();
      if (records != null) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            Set<String> changedKeys = cache.writeTransaction(new Transaction<WriteableCache, Set<String>>() {
              @Nullable @Override public Set<String> execute(WriteableCache cache) {
                return cache.merge(records);
              }
            });
            cache.publish(changedKeys);
          }
        });
      }

      return networkResponse;
    } catch (Exception e) {
      InterceptorResponse response = resolveNetworkFirstCacheResponse(operation, networkResponse);
      if (response != null) {
        return response;
      }
      throw e;
    }
  }

  private InterceptorResponse resolveNetworkFirstCacheResponse(Operation operation,
      InterceptorResponse networkResponse) {
    if (cacheControl == CacheControl.NETWORK_FIRST) {
      ResponseNormalizer<Record> responseNormalizer = cache.cacheResponseNormalizer();
      Response cachedResponse = cachedResponse(operation, responseNormalizer);
      if (cachedResponse != null && cachedResponse.data() != null && cachedResponse.isSuccessful()) {
        okhttp3.Response httpResponse = networkResponse != null ? networkResponse.httpResponse.orNull() : null;
        return new InterceptorResponse(httpResponse, cachedResponse, responseNormalizer.records());
      }
    }
    return null;
  }
}
