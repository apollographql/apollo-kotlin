package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.util.ApolloLogger;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

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
 * server. It is the last interceptor in the chain of interceptors and hence doesn't call
 * {@link ApolloInterceptorChain#proceed()} on the interceptor chain.
 */
@SuppressWarnings("WeakerAccess") public final class ApolloServerInterceptor implements ApolloInterceptor {
  private static final String ACCEPT_TYPE = "application/json";
  private static final String CONTENT_TYPE = "application/json";
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

  final HttpUrl serverUrl;
  final okhttp3.Call.Factory httpCallFactory;
  final Optional<HttpCachePolicy.Policy> cachePolicy;
  final boolean prefetch;
  final Moshi moshi;
  final ApolloLogger logger;
  volatile Call httpCall;

  public ApolloServerInterceptor(@Nonnull HttpUrl serverUrl, @Nonnull Call.Factory httpCallFactory,
      @Nullable HttpCachePolicy.Policy cachePolicy, boolean prefetch, @Nonnull Moshi moshi,
      @Nonnull ApolloLogger logger) {
    this.serverUrl = checkNotNull(serverUrl, "serverUrl == null");
    this.httpCallFactory = checkNotNull(httpCallFactory, "httpCallFactory == null");
    this.cachePolicy = Optional.fromNullable(cachePolicy);
    this.prefetch = prefetch;
    this.moshi = checkNotNull(moshi, "moshi == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Override @Nonnull public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain)
      throws ApolloException {
    try {
      httpCall = httpCall(operation);
    } catch (IOException e) {
      logger.e(e, "Failed to prepare http call");
      throw new ApolloNetworkException("Failed to prepare http call", e);
    }

    try {
      return new InterceptorResponse(httpCall.execute());
    } catch (IOException e) {
      logger.e(e, "Failed to execute http call");
      throw new ApolloNetworkException("Failed to execute http call", e);
    }
  }

  @Override
  public void interceptAsync(@Nonnull final Operation operation, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        try {
          httpCall = httpCall(operation);
        } catch (IOException e) {
          logger.e(e, "Failed to prepare http call");
          callBack.onFailure(new ApolloNetworkException("Failed to prepare http call", e));
          return;
        }

        httpCall.enqueue(new Callback() {
          @Override public void onFailure(Call call, IOException e) {
            logger.e(e, "Failed to execute http call");
            callBack.onFailure(new ApolloNetworkException("Failed to execute http call", e));
          }

          @Override public void onResponse(Call call, Response response) throws IOException {
            callBack.onResponse(new ApolloInterceptor.InterceptorResponse(response));
          }
        });
      }
    });
  }

  @Override public void dispose() {
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
        .header("Accept", ACCEPT_TYPE)
        .header("Content-Type", CONTENT_TYPE);

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
    JsonAdapter<Operation> adapter = new OperationJsonAdapter(moshi);
    Buffer buffer = new Buffer();
    adapter.toJson(buffer, operation);
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }

  public static String cacheKey(RequestBody requestBody) throws IOException {
    Buffer hashBuffer = new Buffer();
    requestBody.writeTo(hashBuffer);
    return hashBuffer.readByteString().md5().hex();
  }

  static final class OperationJsonAdapter extends JsonAdapter<Operation> {
    private final Moshi moshi;

    OperationJsonAdapter(Moshi moshi) {
      this.moshi = moshi;
    }

    @Override public Operation fromJson(JsonReader reader) throws IOException {
      throw new IllegalStateException("This should not be called ever.");
    }

    @Override public void toJson(JsonWriter writer, Operation value) throws IOException {
      writer.beginObject();
      writer.name("query").value(value.queryDocument().replaceAll("\\n", ""));
      Operation.Variables variables = value.variables();
      if (variables != null) {
        //noinspection unchecked
        JsonAdapter<Operation.Variables> adapter =
            (JsonAdapter<Operation.Variables>) moshi.adapter(variables.getClass());
        writer.name("variables");
        adapter.toJson(writer, variables);
      }
      writer.endObject();
    }
  }
}
