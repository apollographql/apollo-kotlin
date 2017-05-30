package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * ApolloParseInterceptor is a concrete {@link ApolloInterceptor} responsible for inflating the http responses into
 * models. To get the http responses, it hands over the control to the next interceptor in the chain and proceeds to
 * then parse the returned response.
 */
public final class ApolloParseInterceptor implements ApolloInterceptor {
  private final HttpCache httpCache;
  private final ResponseNormalizer<Map<String, Object>> normalizer;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final ApolloLogger logger;

  public ApolloParseInterceptor(HttpCache httpCache, ResponseNormalizer<Map<String, Object>> normalizer,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
      ApolloLogger logger) {
    this.httpCache = httpCache;
    this.normalizer = normalizer;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.logger = logger;
  }

  @Override @Nonnull public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain)
      throws ApolloException {
    InterceptorResponse response = chain.proceed();
    return parse(operation, response.httpResponse.get());
  }

  @Override
  public void interceptAsync(@Nonnull final Operation operation, @Nonnull ApolloInterceptorChain chain,
      @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
    chain.proceedAsync(dispatcher, new CallBack() {
      @Override public void onResponse(@Nonnull InterceptorResponse response) {
        try {
          InterceptorResponse result = parse(operation, response.httpResponse.get());
          callBack.onResponse(result);
        } catch (ApolloException e) {
          onFailure(e);
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        callBack.onFailure(e);
      }
    });
  }

  @Override public void dispose() {
    //no op
  }

  @SuppressWarnings("unchecked") private InterceptorResponse parse(Operation operation, okhttp3.Response httpResponse)
      throws ApolloHttpException, ApolloParseException {
    String cacheKey = httpResponse.request().header(HttpCache.CACHE_KEY_HEADER);
    if (httpResponse.isSuccessful()) {
      try {
        HttpResponseBodyParser parser = new HttpResponseBodyParser(operation, responseFieldMapper, customTypeAdapters);
        Response parsedResponse = parser.parse(httpResponse.body(), normalizer);
        if (parsedResponse.hasErrors() && httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        return new InterceptorResponse(httpResponse, parsedResponse, normalizer.records());
      } catch (Exception rethrown) {
        logger.e(rethrown, "Failed to parse network response for operation: %s", operation);
        closeQuietly(httpResponse);
        if (httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        throw new ApolloParseException("Failed to parse http response", rethrown);
      }
    } else {
      logger.e("Failed to parse network response: %s", httpResponse);
      throw new ApolloHttpException(httpResponse);
    }
  }

  private static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ignored) {
      }
    }
  }
}
