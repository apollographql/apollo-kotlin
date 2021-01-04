package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.internal.ApolloLogger;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.http.OkHttpExecutionContext;
import com.apollographql.apollo.response.OperationResponseParser;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * ApolloParseInterceptor is a concrete {@link ApolloInterceptor} responsible for inflating the http responses into
 * models. To get the http responses, it hands over the control to the next interceptor in the chain and proceeds to
 * then parse the returned response.
 */
public final class ApolloParseInterceptor implements ApolloInterceptor {
  private final HttpCache httpCache;
  private final ResponseNormalizer<Map<String, Object>> normalizer;
  private final ResponseFieldMapper responseFieldMapper;
  private final CustomScalarAdapters customScalarAdapters;
  private final ApolloLogger logger;
  volatile boolean disposed;

  public ApolloParseInterceptor(HttpCache httpCache, ResponseNormalizer<Map<String, Object>> normalizer,
      ResponseFieldMapper responseFieldMapper, CustomScalarAdapters customScalarAdapters, ApolloLogger logger) {
    this.httpCache = httpCache;
    this.normalizer = normalizer;
    this.responseFieldMapper = responseFieldMapper;
    this.customScalarAdapters = customScalarAdapters;
    this.logger = logger;
  }

  @Override
  public void interceptAsync(@NotNull final InterceptorRequest request, @NotNull ApolloInterceptorChain chain,
      @NotNull Executor dispatcher, @NotNull final CallBack callBack) {
    if (disposed) return;
    chain.proceedAsync(request, dispatcher, new CallBack() {
      @Override public void onResponse(@NotNull InterceptorResponse response) {
        try {
          if (disposed) return;
          InterceptorResponse result = parse(request.operation, response.httpResponse.get());
          callBack.onResponse(result);
          callBack.onCompleted();
        } catch (ApolloException e) {
          onFailure(e);
        }
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        if (disposed) return;
        callBack.onFailure(e);
      }

      @Override public void onCompleted() {
        // call onCompleted in onResponse in case of error
      }

      @Override public void onFetch(FetchSourceType sourceType) {
        callBack.onFetch(sourceType);
      }
    });
  }

  @Override public void dispose() {
    disposed = true;
  }

  @SuppressWarnings("unchecked") InterceptorResponse parse(Operation operation, okhttp3.Response httpResponse)
      throws ApolloHttpException, ApolloParseException {
    String cacheKey = httpResponse.request().header(HttpCache.CACHE_KEY_HEADER);
    if (httpResponse.isSuccessful()) {
      try {
        final OperationResponseParser parser = new OperationResponseParser(operation, responseFieldMapper, customScalarAdapters, normalizer);
        final OkHttpExecutionContext httpExecutionContext = new OkHttpExecutionContext(httpResponse);
        Response parsedResponse = parser.parse(httpResponse.body().source());
        parsedResponse = parsedResponse
            .toBuilder()
            .fromCache(httpResponse.cacheResponse() != null)
            .executionContext(parsedResponse.getExecutionContext().plus(httpExecutionContext))
            .build();

        if (parsedResponse.hasErrors() && httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        return new InterceptorResponse(httpResponse, parsedResponse, normalizer.records());
      } catch (Exception rethrown) {
        logger.e(rethrown, "Failed to parse network response for operation: %s", operation.name().name());
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
