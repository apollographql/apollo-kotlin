package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.CustomScalarType;
import com.apollographql.apollo3.api.ExecutionContext;
import com.apollographql.apollo3.api.MutableExecutionOptions;
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
import com.apollographql.apollo3.runtime.java.internal.AutoPersistedQueryInterceptor;
import com.apollographql.apollo3.runtime.java.internal.DefaultApolloCall;
import com.apollographql.apollo3.runtime.java.internal.DefaultApolloDisposable;
import com.apollographql.apollo3.runtime.java.internal.DefaultInterceptorChain;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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
  private CustomScalarAdapters customScalarAdapters;

  private ApolloClient(
      String serverUrl,
      Call.Factory callFactory,
      Executor executor,
      List<ApolloInterceptor> interceptors,
      CustomScalarAdapters customScalarAdapters
  ) {
    this.serverUrl = serverUrl;
    this.callFactory = callFactory;
    this.executor = executor;
    this.interceptors = interceptors;
    this.customScalarAdapters = customScalarAdapters;
  }

  public <D extends Query.Data> ApolloCall<D> query(@NotNull Query<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Mutation.Data> ApolloCall<D> mutation(@NotNull Mutation<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Operation.Data> ApolloDisposable execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback) {
    DefaultApolloDisposable disposable = new DefaultApolloDisposable();

    ArrayList<ApolloInterceptor> interceptors = new ArrayList<>(this.interceptors);
    interceptors.add(new NetworkInterceptor(callFactory, serverUrl, customScalarAdapters));

    executor.execute(() -> new DefaultInterceptorChain(interceptors, 0, disposable).proceed(request, callback));

    return disposable;
  }

  private static class NetworkInterceptor implements ApolloInterceptor {
    private Call.Factory callFactory;
    private HttpRequestComposer requestComposer;
    private CustomScalarAdapters customScalarAdapters;

    private NetworkInterceptor(Call.Factory callFactory, String serverUrl, CustomScalarAdapters customScalarAdapters) {
      this.callFactory = callFactory;
      this.requestComposer = new DefaultHttpRequestComposer(serverUrl);
      this.customScalarAdapters = customScalarAdapters;
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

      Call call = callFactory.newCall(builder.build());

      ApolloDisposable.Listener listener = () -> call.cancel();

      ApolloDisposable disposable = chain.getDisposable();
      disposable.addListener(listener);

      try {
        // It might be that we are cancelled even before we registered the listener
        // Do an early check for that case
        if (disposable.isDisposed()) {
          return;
        }
        Response response = call.execute();
        if (!response.isSuccessful()) {
          ArrayList<HttpHeader> headers = new ArrayList<>();
          response.headers().forEach(pair -> headers.add(new HttpHeader(pair.getFirst(), pair.getSecond())));

          // TODO: include body in exception
          callback.onFailure(new ApolloHttpException(response.code(), headers, null, "HTTP error", null));
        } else {
          BufferedSourceJsonReader jsonReader = new BufferedSourceJsonReader(response.body().source());
          try {
            ApolloResponse<D> apolloResponse = Operations.parseJsonResponse(request.getOperation(), jsonReader, customScalarAdapters);
            callback.onResponse(apolloResponse);
          } catch (Exception e) {
            if (disposable.isDisposed()) {
              return;
            }
            callback.onFailure(new ApolloParseException("Cannot parse response", e));
          }
        }
      } catch (IOException e) {
        if (disposable.isDisposed()) {
          return;
        }
        callback.onFailure(new ApolloNetworkException("Network error", e));
      } finally {
        chain.getDisposable().removeListener(listener);
      }
    }
  }


  public static class Builder implements MutableExecutionOptions<Builder> {
    private String serverUrl;
    private Call.Factory callFactory;
    private Executor executor;
    private List<ApolloInterceptor> interceptors = new ArrayList<>();
    private final CustomScalarAdapters.Builder customScalarAdaptersBuilder = new CustomScalarAdapters.Builder();
    private ExecutionContext executionContext;
    private HttpMethod httpMethod;
    private final ArrayList<HttpHeader> httpHeaders = new ArrayList<>();
    private Boolean sendApqExtensions;
    private Boolean sendDocument;
    private Boolean enableAutoPersistedQueries;
    private Boolean canBeBatched;

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
     * The {@link Executor} to use for dispatching the requests.
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

    public Builder customScalarAdapters(@NotNull CustomScalarAdapters customScalarAdapters) {
      this.customScalarAdaptersBuilder.clear();
      this.customScalarAdaptersBuilder.addAll(customScalarAdapters);
      return this;
    }

    /**
     * Registers the given customScalarAdapter.
     *
     * @param customScalarType a generated {@link CustomScalarType}. Every GraphQL custom scalar has a generated class with a static `type`
     * property. For an example, for a `Date` custom scalar, you can use `com.example.Date.type`
     * @param customScalarAdapter the {@link Adapter} to use for this custom scalar
     */
    public <T> Builder addCustomScalarAdapter(@NotNull CustomScalarType customScalarType, @NotNull Adapter<T> customScalarAdapter) {
      customScalarAdaptersBuilder.add(customScalarType, customScalarAdapter);
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

      return new ApolloClient(serverUrl, callFactory, executor, interceptors, customScalarAdaptersBuilder.build());
    }

    public Builder autoPersistedQueries() {
      return autoPersistedQueries(HttpMethod.Get, HttpMethod.Post, true);
    }

    public Builder autoPersistedQueries(
        HttpMethod httpMethodForHashedQueries
    ) {
      return autoPersistedQueries(httpMethodForHashedQueries, HttpMethod.Post, true);
    }

    public Builder autoPersistedQueries(
        HttpMethod httpMethodForHashedQueries,
        HttpMethod httpMethodForDocumentQueries
    ) {
      return autoPersistedQueries(httpMethodForHashedQueries, httpMethodForDocumentQueries, true);
    }

    public Builder autoPersistedQueries(
        HttpMethod httpMethodForHashedQueries,
        HttpMethod httpMethodForDocumentQueries,
        boolean enableByDefault
    ) {
      addInterceptor(
          new AutoPersistedQueryInterceptor(
              httpMethodForHashedQueries,
              httpMethodForDocumentQueries
          )
      );
      enableAutoPersistedQueries(enableByDefault);

      return this;
    }

    @NotNull @Override public ExecutionContext getExecutionContext() {
      return executionContext;
    }

    @Nullable @Override public HttpMethod getHttpMethod() {
      return httpMethod;
    }

    @Nullable @Override public List<HttpHeader> getHttpHeaders() {
      return httpHeaders;
    }

    @Nullable @Override public Boolean getSendApqExtensions() {
      return sendApqExtensions;
    }

    @Nullable @Override public Boolean getSendDocument() {
      return sendDocument;
    }

    @Nullable @Override public Boolean getEnableAutoPersistedQueries() {
      return enableAutoPersistedQueries;
    }

    @Nullable @Override public Boolean getCanBeBatched() {
      return canBeBatched;
    }

    @Override public Builder addExecutionContext(@NotNull ExecutionContext executionContext) {
      this.executionContext = this.executionContext.plus(executionContext);
      return this;
    }

    @Override public Builder httpMethod(@Nullable HttpMethod httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    @Override public Builder httpHeaders(@Nullable List<HttpHeader> list) {
      this.httpHeaders.clear();
      this.httpHeaders.addAll(list);
      return this;
    }

    @Override public Builder addHttpHeader(@NotNull String name, @NotNull String value) {
      this.httpHeaders.add(new HttpHeader(name, value));
      return this;
    }

    @Override public Builder sendApqExtensions(@Nullable Boolean sendApqExtensions) {
      this.sendApqExtensions = sendApqExtensions;
      return this;
    }

    @Override public Builder sendDocument(@Nullable Boolean sendDocument) {
      this.sendDocument = sendDocument;
      return this;
    }

    @Override public Builder enableAutoPersistedQueries(@Nullable Boolean enableAutoPersistedQueries) {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries;
      return this;
    }

    @Override public Builder canBeBatched(@Nullable Boolean canBeBatched) {
      throw new NotImplementedException();
    }

  }

  static private Executor defaultExecutor() {
    return newCachedThreadPool(runnable -> new Thread(runnable, "Apollo Dispatcher"));
  }
}
