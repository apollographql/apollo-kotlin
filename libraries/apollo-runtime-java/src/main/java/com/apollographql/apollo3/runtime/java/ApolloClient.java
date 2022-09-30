package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Operations;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.api.http.HttpRequest;
import com.apollographql.apollo3.api.http.HttpRequestComposer;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.exception.ApolloHttpException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.ApolloParseException;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptorChain;
import kotlin.Pair;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.apollographql.apollo3.api.java.Assertions.checkNotNull;

public class ApolloClient {
  private String serverUrl;
  private Call.Factory callFactory;
  private Executor executor;

  private ApolloClient(
      String serverUrl,
      Call.Factory callFactory,
      Executor executor
  ) {
    this.serverUrl = serverUrl;
    this.callFactory = callFactory;
    this.executor = executor;
  }

  public <D extends Query.Data> ApolloCall<D> query(Operation<D> operation) {
    return new DefaultApolloCall(this, operation);
  }

  public <D extends Query.Data> ApolloDisposable execute(ApolloRequest<D> request, ApolloCallback<D> callback) {
    DefaultApolloDisposable disposable = new DefaultApolloDisposable();

    ArrayList<ApolloInterceptor> interceptors = new ArrayList<>();
    interceptors.add(new NetworkInterceptor(callFactory, serverUrl));

    ApolloCallback<D> callbackWrapper = new ApolloCallback<D>() {

      @Override public void onResponse(@NotNull ApolloResponse<D> response) {
        callback.onResponse(response);
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        callback.onFailure(e);
      }
    };

    executor.execute(() -> new DefaultInterceptorChain(interceptors, 0, disposable).proceed(request, callbackWrapper));

    return disposable;
  }

  private static class NetworkInterceptor implements ApolloInterceptor {
    private Call.Factory callFactory;
    private HttpRequestComposer requestComposer;

    private NetworkInterceptor(Call.Factory callFactory, String serverUrl) {
      this.callFactory = callFactory;
      this.requestComposer = new DefaultHttpRequestComposer(serverUrl);
    }

    @Override public void intercept(@NotNull ApolloRequest request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback callback) {
      HttpRequest httpRequest = requestComposer.compose(request);
      Request.Builder builder = new Request.Builder()
          .url(httpRequest.getUrl());

      httpRequest.getHeaders().forEach(httpHeader -> {
            builder.addHeader(httpHeader.getName(), httpHeader.getValue());
          }
      );

      if (httpRequest.getMethod() == HttpMethod.Post) {
        builder.post(new RequestBody() {
          @Nullable @Override public MediaType contentType() {
            return MediaType.parse(httpRequest.getBody().getContentType());
          }
          @Override public long contentLength() {
            return httpRequest.getBody().getContentLength();
          }

          @Override public void writeTo(@NotNull BufferedSink bufferedSink) throws IOException {
            httpRequest.getBody().writeTo(bufferedSink);
          }
        });
      }

      try {
        Response response = callFactory.newCall(builder.build()).execute();
        if (!response.isSuccessful()) {
          ArrayList<HttpHeader> headers = new ArrayList<>();
          response.headers().forEach(pair -> headers.add(new HttpHeader(pair.getFirst(), pair.getSecond())));

          callback.onFailure(new ApolloHttpException(response.code(), headers, null, "HTTP error", null));
        } else {
          BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(response.body().source());
          try {
            ApolloResponse apolloResponse = Operations.parseJsonResponse(request.getOperation(), jsonReader, CustomScalarAdapters.Empty);
            callback.onResponse(apolloResponse);
          } catch (Exception e) {
            callback.onFailure(new ApolloParseException("Cannot parse response", e));
          }
        }
      } catch (IOException e) {
        callback.onFailure(new ApolloNetworkException("Network error", e));
      }
    }
  }


  public static class Builder {
    private String serverUrl;
    private Call.Factory callFactory;
    private Executor executor;

    public Builder() {
    }

    public Builder serverUrl(@NotNull String serverUrl) {
      this.serverUrl = serverUrl;
      return this;
    }

    /**
     * Set the {@link OkHttpClient} to use for making network requests.
     *
     * @param okHttpClient the client to use.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder okHttpClient(@NotNull OkHttpClient okHttpClient) {
      this.callFactory = checkNotNull(okHttpClient, "okHttpClient is null");
      return null;
    }

    /**
     * Set the custom call factory for creating {@link Call} instances. <p> Note: Calling {@link #okHttpClient(OkHttpClient)} automatically
     * sets this value.
     */
    public Builder callFactory(@NotNull Call.Factory factory) {
      this.callFactory = checkNotNull(factory, "factory is null");
      return this;
    }

    /**
     * The #{@link Executor} to use for dispatching the requests.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder dispatcher(@NotNull Executor dispatcher) {
      this.executor = checkNotNull(dispatcher, "dispatcher is null");
      return this;
    }

    public ApolloClient build() {
      checkNotNull(serverUrl, "serverUrl is missing");
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      if (executor == null) {
        executor = defaultExecutor();
      }
      return new ApolloClient(serverUrl, callFactory, executor);
    }
  }

  static private Executor defaultExecutor() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
        new SynchronousQueue<>(), runnable -> new Thread(runnable, "Apollo Dispatcher"));
  }
}