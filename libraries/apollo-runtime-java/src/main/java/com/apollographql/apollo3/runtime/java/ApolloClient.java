package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Operations;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.api.http.HttpRequest;
import com.apollographql.apollo3.api.http.HttpRequestComposer;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.exception.ApolloHttpException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.ApolloParseException;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptorChain;
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
import java.util.List;
import java.util.concurrent.Executor;

import static com.apollographql.apollo3.api.java.Assertions.checkNotNull;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class ApolloClient {
  private String serverUrl;
  private Call.Factory callFactory;
  private Executor executor;
  private List<ApolloInterceptor> interceptors;


  private ApolloClient(
      String serverUrl,
      Call.Factory callFactory,
      Executor executor,
      List<ApolloInterceptor> interceptors
  ) {
    this.serverUrl = serverUrl;
    this.callFactory = callFactory;
    this.executor = executor;
    this.interceptors = interceptors;
  }

  public <D extends Query.Data> ApolloCall<D> query(Query<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Mutation.Data> ApolloCall<D> mutation(Mutation<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Operation.Data> ApolloDisposable execute(ApolloRequest<D> request, ApolloCallback<D> callback) {
    DefaultApolloDisposable disposable = new DefaultApolloDisposable();

    ArrayList<ApolloInterceptor> interceptors = new ArrayList<>(this.interceptors);
    interceptors.add(new NetworkInterceptor(callFactory, serverUrl));

    executor.execute(() -> new DefaultInterceptorChain(interceptors, 0, disposable).proceed(request, callback));

    return disposable;
  }

  private static class NetworkInterceptor implements ApolloInterceptor {
    private Call.Factory callFactory;
    private HttpRequestComposer requestComposer;

    private NetworkInterceptor(Call.Factory callFactory, String serverUrl) {
      this.callFactory = callFactory;
      this.requestComposer = new DefaultHttpRequestComposer(serverUrl);
    }

    @Override
    public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
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

          // TODO: include body in exception
          callback.onFailure(new ApolloHttpException(response.code(), headers, null, "HTTP error", null));
        } else {
          BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(response.body().source());
          try {
            ApolloResponse<D> apolloResponse = Operations.parseJsonResponse(request.getOperation(), jsonReader, CustomScalarAdapters.Empty);
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
    private List<ApolloInterceptor> interceptors = new ArrayList<>();

    public Builder() {
    }

    public Builder serverUrl(@NotNull String serverUrl) {
      this.serverUrl = checkNotNull(serverUrl, "serverUrl is null");
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

    public Builder addInterceptor(@NotNull ApolloInterceptor interceptor) {
      this.interceptors.add(checkNotNull(interceptor, "interceptor is null"));
      return this;
    }

    public Builder addInterceptors(@NotNull List<ApolloInterceptor> interceptors) {
      this.interceptors.addAll(checkNotNull(interceptors, "interceptors is null"));
      return this;
    }

    public Builder interceptors(@NotNull List<ApolloInterceptor> interceptors) {
      this.interceptors = checkNotNull(interceptors, "interceptors is null");
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
      return new ApolloClient(serverUrl, callFactory, executor, interceptors);
    }
  }

  static private Executor defaultExecutor() {
    return newCachedThreadPool(runnable -> new Thread(runnable, "Apollo Dispatcher"));
  }
}
