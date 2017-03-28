package com.apollographql.android.impl;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.apollographql.android.cache.http.HttpCache;
import com.apollographql.android.cache.normalized.ResponseNormalizer;
import com.apollographql.android.impl.util.HttpException;
import com.apollographql.android.internal.ApolloLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

final class ApolloParseInterceptor implements ApolloInterceptor {
  private final HttpCache httpCache;
  private final ResponseNormalizer<Map<String, Object>> normalizer;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final ApolloLogger logger;

  ApolloParseInterceptor(HttpCache httpCache, ResponseNormalizer<Map<String, Object>> normalizer,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
      ApolloLogger logger) {
    this.httpCache = httpCache;
    this.normalizer = normalizer;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.logger = logger;
  }

  @Override @Nonnull public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain)
      throws IOException {
    InterceptorResponse response = chain.proceed();
    return parse(operation, response.httpResponse.get());
  }

  @Override
  public void interceptAsync(@Nonnull final Operation operation, @Nonnull ApolloInterceptorChain chain,
      @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
    chain.proceedAsync(dispatcher, new CallBack() {
      @Override public void onResponse(@Nonnull InterceptorResponse response) {
        try {
          callBack.onResponse(parse(operation, response.httpResponse.get()));
        } catch (Exception e) {
          callBack.onFailure(e);
        }
      }

      @Override public void onFailure(@Nonnull Throwable t) {
        callBack.onFailure(t);
      }
    });
  }

  @Override public void dispose() {
    //no op
  }

  @SuppressWarnings("unchecked") private InterceptorResponse parse(Operation operation, okhttp3.Response
      httpResponse) throws IOException {
    String cacheKey = httpResponse.request().header(HttpCache.CACHE_KEY_HEADER);
    if (httpResponse.isSuccessful()) {
      try {
        HttpResponseBodyParser parser = new HttpResponseBodyParser(operation, responseFieldMapper, customTypeAdapters);
        Response parsedResponse = parser.parse(httpResponse.body(), normalizer);
        return new InterceptorResponse(httpResponse, parsedResponse, normalizer.records());
      } catch (Exception rethrown) {
        logger.e(rethrown, "Failed to parse network response for operation %s", operation);
        closeQuietly(httpResponse);
        if (httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        throw rethrown;
      }
    } else {
      throw new HttpException(httpResponse);
    }
  }

  private static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (RuntimeException rethrown) {
        throw rethrown;
      } catch (Exception ignored) {
      }
    }
  }
}
