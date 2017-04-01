package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.normalized.Cache;
import com.apollographql.apollo.internal.cache.normalized.ReadableCache;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.cache.normalized.Transaction;
import com.apollographql.apollo.internal.cache.normalized.WriteableCache;
import com.apollographql.apollo.internal.field.CacheFieldValueResolver;
import com.apollographql.apollo.internal.reader.RealResponseReader;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class ApolloCacheInterceptor implements ApolloInterceptor {
  private final Cache cache;
  private final CacheControl cacheControl;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final ExecutorService dispatcher;
  private final ApolloLogger logger;

  public ApolloCacheInterceptor(@Nonnull Cache cache, @Nonnull CacheControl cacheControl,
      @Nonnull ResponseFieldMapper responseFieldMapper,
      @Nonnull Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
      @Nonnull ExecutorService dispatcher, @Nonnull ApolloLogger logger) {
    this.cache = checkNotNull(cache, "cache == null");
    this.cacheControl = checkNotNull(cacheControl, "cacheControl == null");
    this.responseFieldMapper = checkNotNull(responseFieldMapper, "responseFieldMapper == null");
    this.customTypeAdapters = checkNotNull(customTypeAdapters, "customTypeAdapters == null");
    this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Nonnull @Override public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain)
      throws ApolloException {
    InterceptorResponse cachedResponse = resolveCacheFirstResponse(operation);
    if (cachedResponse != null) {
      return cachedResponse;
    }

    InterceptorResponse networkResponse;
    try {
      networkResponse = chain.proceed();
    } catch (Exception e) {
      InterceptorResponse networkFirstCacheResponse = resolveNetworkFirstCacheResponse(operation);
      if (networkFirstCacheResponse != null) {
        logger.d(e, "Failed to fetch network response for operation %s, return cached one", operation);
        return networkFirstCacheResponse;
      }
      throw e;
    }
    return handleNetworkResponse(operation, networkResponse);
  }

  @Override
  public void interceptAsync(@Nonnull final Operation operation, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull final ExecutorService dispatcher, @Nonnull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        final InterceptorResponse cachedResponse = resolveCacheFirstResponse(operation);
        if (cachedResponse != null) {
          callBack.onResponse(cachedResponse);
          return;
        }

        chain.proceedAsync(dispatcher, new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {
            callBack.onResponse(handleNetworkResponse(operation, response));
          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            InterceptorResponse response = resolveNetworkFirstCacheResponse(operation);
            if (response != null) {
              logger.d(e, "Failed to fetch network response for operation %s, return cached one", operation);
              callBack.onResponse(response);
            } else {
              callBack.onFailure(e);
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
        logger.d("Cache HIT for operation %s", operation);
        return new InterceptorResponse(null, cachedResponse, responseNormalizer.records());
      }
    }
    logger.d("Cache MISS for operation %s", operation);
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
        } catch (final Exception e) {
          logger.e(e, "Failed to parse cached response for operation: %s", operation);
          return new Response(operation);
        }
      }
    });
  }

  private InterceptorResponse handleNetworkResponse(Operation operation, InterceptorResponse networkResponse) {
    boolean networkFailed = (!networkResponse.httpResponse.isPresent()
        || !networkResponse.httpResponse.get().isSuccessful());
    if (networkFailed && cacheControl != CacheControl.NETWORK_ONLY) {
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
  }

  private InterceptorResponse resolveNetworkFirstCacheResponse(Operation operation) {
    if (cacheControl == CacheControl.NETWORK_FIRST) {
      ResponseNormalizer<Record> responseNormalizer = cache.cacheResponseNormalizer();
      Response cachedResponse = cachedResponse(operation, responseNormalizer);
      if (cachedResponse != null && cachedResponse.data() != null && cachedResponse.isSuccessful()) {
        logger.d("Cache HIT for operation %s", operation);
        return new InterceptorResponse(null, cachedResponse, responseNormalizer.records());
      }
    }
    logger.d("Cache MISS for operation %s", operation);
    return null;
  }
}
