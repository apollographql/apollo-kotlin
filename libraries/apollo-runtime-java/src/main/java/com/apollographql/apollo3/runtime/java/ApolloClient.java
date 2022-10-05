package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.CustomScalarType;
import com.apollographql.apollo3.api.ExecutionContext;
import com.apollographql.apollo3.api.MutableExecutionOptions;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.api.Subscription;
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpRequestComposer;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo3.runtime.java.internal.http.HttpNetworkTransport;
import com.apollographql.apollo3.runtime.java.internal.ws.ApolloWsProtocol;
import com.apollographql.apollo3.runtime.java.internal.ws.WebSocketNetworkTransport;
import com.apollographql.apollo3.runtime.java.internal.ws.WsProtocol;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.exception.ApolloHttpException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.ApolloParseException;
import com.apollographql.apollo3.runtime.java.internal.AutoPersistedQueryInterceptor;
import com.apollographql.apollo3.runtime.java.internal.DefaultApolloCall;
import com.apollographql.apollo3.runtime.java.internal.DefaultApolloDisposable;
import com.apollographql.apollo3.runtime.java.internal.DefaultInterceptorChain;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static com.apollographql.apollo3.api.java.Assertions.checkNotNull;
import static java.util.concurrent.Executors.newCachedThreadPool;

public class ApolloClient {
  private String serverUrl;
  private String webSocketServerUrl;
  private Call.Factory callFactory;
  private WebSocket.Factory webSocketFactory;
  private Executor executor;
  private List<ApolloInterceptor> interceptors;
  private CustomScalarAdapters customScalarAdapters;
  private WsProtocol.Factory wsProtocolFactory;
  private List<HttpHeader> wsHeaders;

  private ApolloClient(
      String serverUrl,
      String webSocketServerUrl,
      Call.Factory callFactory,
      WebSocket.Factory webSocketFactory,
      Executor executor,
      List<ApolloInterceptor> interceptors,
      CustomScalarAdapters customScalarAdapters,
      WsProtocol.Factory wsProtocolFactory,
      List<HttpHeader> wsHeaders
  ) {
    this.serverUrl = serverUrl;
    this.webSocketServerUrl = webSocketServerUrl;
    this.callFactory = callFactory;
    this.webSocketFactory = webSocketFactory;
    this.executor = executor;
    this.interceptors = interceptors;
    this.customScalarAdapters = customScalarAdapters;
    this.wsProtocolFactory = wsProtocolFactory;
    this.wsHeaders = wsHeaders;
  }

  public <D extends Query.Data> ApolloCall<D> query(@NotNull Query<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Mutation.Data> ApolloCall<D> mutation(@NotNull Mutation<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Subscription.Data> ApolloCall<D> subscription(@NotNull Subscription<D> operation) {
    return new DefaultApolloCall<>(this, operation);
  }

  public <D extends Operation.Data> ApolloDisposable execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback) {
    DefaultApolloDisposable disposable = new DefaultApolloDisposable();

    ArrayList<ApolloInterceptor> interceptors = new ArrayList<>(this.interceptors);
    interceptors.add(new NetworkInterceptor(
        callFactory,
        webSocketFactory,
        serverUrl,
        webSocketServerUrl,
        customScalarAdapters,
        wsProtocolFactory,
        wsHeaders,
        executor
    ));

    executor.execute(() -> new DefaultInterceptorChain(interceptors, 0, disposable).proceed(request, callback));

    return disposable;
  }

  private static class NetworkInterceptor implements ApolloInterceptor {
    private HttpNetworkTransport httpNetworkTransport;
    private WebSocketNetworkTransport webSocketNetworkTransport;

    private NetworkInterceptor(
        Call.Factory callFactory,
        WebSocket.Factory webSocketFactory,
        String serverUrl,
        String webSocketServerUrl,
        CustomScalarAdapters customScalarAdapters,
        WsProtocol.Factory wsProtocolFactory,
        List<HttpHeader> wsHeaders,
        Executor executor
    ) {
      HttpRequestComposer httpRequestComposer = new DefaultHttpRequestComposer(serverUrl);
      httpNetworkTransport = new HttpNetworkTransport(callFactory, httpRequestComposer, customScalarAdapters);
      webSocketNetworkTransport = new WebSocketNetworkTransport(webSocketFactory, wsProtocolFactory, webSocketServerUrl, wsHeaders, executor);
    }

    @Override
    public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
      if (request.getOperation() instanceof Query || request.getOperation() instanceof Mutation) {
        httpNetworkTransport.execute(request, callback, chain.getDisposable());
      } else {
        webSocketNetworkTransport.execute(request, callback, chain.getDisposable());
      }
    }
  }

  public CustomScalarAdapters getCustomScalarAdapters() {
    return customScalarAdapters;
  }


  public static class Builder implements MutableExecutionOptions<Builder> {
    private String serverUrl;
    private String webSocketServerUrl;
    private Call.Factory callFactory;
    private WebSocket.Factory webSocketFactory;
    private Executor executor;
    private List<ApolloInterceptor> interceptors = new ArrayList<>();
    private CustomScalarAdapters.Builder customScalarAdaptersBuilder = new CustomScalarAdapters.Builder();
    private WsProtocol.Factory wsProtocolFactory;
    private List<HttpHeader> wsHeaders = new ArrayList<>();
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

    public Builder webSocketServerUrl(@NotNull String webSocketServerUrl) {
      this.webSocketServerUrl = checkNotNull(webSocketServerUrl, "webSocketServerUrl is null");
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
      this.webSocketFactory = okHttpClient;
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
     * Set the custom call factory for creating {@link WebSocket} instances. <p> Note: Calling {@link #okHttpClient(OkHttpClient)}
     * automatically sets this value.
     */
    public Builder webSocketFactory(@NotNull WebSocket.Factory factory) {
      this.webSocketFactory = checkNotNull(factory, "factory is null");
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

    public Builder wsProtocolFactory(@NotNull WsProtocol.Factory wsProtocolFactory) {
      this.wsProtocolFactory = checkNotNull(wsProtocolFactory, "wsProtocolFactory is null");
      return this;
    }

    public Builder addWsHeader(@NotNull HttpHeader header) {
      this.wsHeaders.add(checkNotNull(header, "header is null"));
      return this;
    }

    public Builder addWsHeaders(@NotNull List<HttpHeader> headers) {
      this.wsHeaders.addAll(checkNotNull(headers, "headers is null"));
      return this;
    }

    public Builder wsHeaders(@NotNull List<HttpHeader> headers) {
      this.wsHeaders = checkNotNull(headers, "headers is null");
      return this;
    }

    public ApolloClient build() {
      checkNotNull(serverUrl, "serverUrl is missing");

      if (webSocketServerUrl == null) {
        webSocketServerUrl = serverUrl;
      }

      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      if (webSocketFactory == null) {
        webSocketFactory = callFactory instanceof OkHttpClient ? (OkHttpClient) callFactory : new OkHttpClient();
      }

      if (executor == null) {
        executor = defaultExecutor();
      }

      if (wsProtocolFactory == null) {
        // TODO change the default to GraphQLWsProtocol.Factory
        wsProtocolFactory = new ApolloWsProtocol.Factory();
      }

      return new ApolloClient(
          serverUrl,
          webSocketServerUrl,
          callFactory,
          webSocketFactory,
          executor,
          interceptors,
          customScalarAdaptersBuilder.build(),
          wsProtocolFactory,
          wsHeaders
      );
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
      throw new UnsupportedOperationException();
    }

  }

  static private Executor defaultExecutor() {
    return newCachedThreadPool(runnable -> new Thread(runnable, "Apollo Dispatcher"));
  }
}
