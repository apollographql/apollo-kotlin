package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.internal.response.ScalarTypeAdapters;
import com.apollographql.apollo.internal.ApolloLogger;

import java.io.IOException;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloServerInterceptor is a concrete {@link ApolloInterceptor} responsible for making the network calls to the
 * server. It is the last interceptor in the chain of interceptors and hence doesn't call {@link
 * ApolloInterceptorChain#proceed(FetchOptions)} on the interceptor chain.
 */
@SuppressWarnings("WeakerAccess") public final class ApolloServerInterceptor implements ApolloInterceptor {
  private static final String HEADER_ACCEPT_TYPE = "Accept";
  private static final String HEADER_CONTENT_TYPE = "CONTENT_TYPE";
  private static final String HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID";
  private static final String ACCEPT_TYPE = "application/json";
  private static final String CONTENT_TYPE = "application/json";
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  final HttpUrl serverUrl;
  final okhttp3.Call.Factory httpCallFactory;
  final Optional<HttpCachePolicy.Policy> cachePolicy;
  final boolean prefetch;
  final ApolloLogger logger;
  final ScalarTypeAdapters scalarTypeAdapters;
  final boolean sendOperationIdentifiers;
  volatile Call httpCall;
  volatile boolean disposed;

  public ApolloServerInterceptor(@Nonnull HttpUrl serverUrl, @Nonnull Call.Factory httpCallFactory,
      @Nullable HttpCachePolicy.Policy cachePolicy, boolean prefetch,
      @Nonnull ScalarTypeAdapters scalarTypeAdapters, @Nonnull ApolloLogger logger,
      boolean sendOperationIdentifiers) {
    this.serverUrl = checkNotNull(serverUrl, "serverUrl == null");
    this.httpCallFactory = checkNotNull(httpCallFactory, "httpCallFactory == null");
    this.cachePolicy = Optional.fromNullable(cachePolicy);
    this.prefetch = prefetch;
    this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    this.logger = checkNotNull(logger, "logger == null");
    this.sendOperationIdentifiers = sendOperationIdentifiers;
  }

  @Override
  public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
    if (disposed) return;
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        callBack.onFetch(FetchSourceType.NETWORK);

        try {
          httpCall = httpCall(request.operation);
        } catch (IOException e) {
          logger.e(e, "Failed to prepare http call for operation %s", request.operation.name().name());
          callBack.onFailure(new ApolloNetworkException("Failed to prepare http call", e));
          return;
        }

        httpCall.enqueue(new Callback() {
          @Override public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
            if (disposed) return;
            logger.e(e, "Failed to execute http call for operation %s", request.operation.name().name());
            callBack.onFailure(new ApolloNetworkException("Failed to execute http call", e));
          }

          @Override public void onResponse(@Nonnull Call call, @Nonnull Response response) throws IOException {
            if (disposed) return;
            callBack.onResponse(new ApolloInterceptor.InterceptorResponse(response));
            callBack.onCompleted();
          }
        });
      }
    });
  }

  @Override public void dispose() {
    disposed = true;
    Call httpCall = this.httpCall;
    if (httpCall != null) {
      httpCall.cancel();
    }
    this.httpCall = null;
  }

  private Call httpCall(Operation operation) throws IOException {
    RequestBody requestBody = httpRequestBody(operation);
    Request.Builder requestBuilder = new Request.Builder()
        .url(serverUrl)
        .post(requestBody)
        .header(HEADER_ACCEPT_TYPE, ACCEPT_TYPE)
        .header(HEADER_CONTENT_TYPE, CONTENT_TYPE)
        .header(HEADER_APOLLO_OPERATION_ID, operation.operationId());

    if (cachePolicy.isPresent()) {
      HttpCachePolicy.Policy cachePolicy = this.cachePolicy.get();
      String cacheKey = cacheKey(requestBody);
      requestBuilder = requestBuilder
          .header(HttpCache.CACHE_KEY_HEADER, cacheKey)
          .header(HttpCache.CACHE_FETCH_STRATEGY_HEADER, cachePolicy.fetchStrategy.name())
          .header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER, String.valueOf(cachePolicy.expireTimeoutMs()))
          .header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER, Boolean.toString(cachePolicy.expireAfterRead))
          .header(HttpCache.CACHE_PREFETCH_HEADER, Boolean.toString(prefetch));
    }

    return httpCallFactory.newCall(requestBuilder.build());
  }

  private RequestBody httpRequestBody(Operation operation) throws IOException {
    Buffer buffer = new Buffer();
    JsonWriter jsonWriter = JsonWriter.of(buffer);
    jsonWriter.beginObject();
    if (sendOperationIdentifiers) {
      jsonWriter.name("id").value(operation.operationId());
    } else {
      jsonWriter.name("query").value(operation.queryDocument().replaceAll("\\n", ""));
    }
    jsonWriter.name("variables").beginObject();
    operation.variables().marshaller().marshal(new InputFieldJsonWriter(jsonWriter, scalarTypeAdapters));
    jsonWriter.endObject();
    jsonWriter.endObject();
    jsonWriter.close();
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }

  public static String cacheKey(RequestBody requestBody) {
    Buffer hashBuffer = new Buffer();
    try {
      requestBody.writeTo(hashBuffer);
    } catch (IOException e) {
      // should never happen
      throw new RuntimeException(e);
    }
    return hashBuffer.readByteString().md5().hex();
  }
}
