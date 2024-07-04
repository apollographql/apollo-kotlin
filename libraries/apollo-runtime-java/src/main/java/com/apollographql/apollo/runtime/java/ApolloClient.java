package com.apollographql.apollo.runtime.java;

import com.apollographql.apollo.annotations.ApolloDeprecatedSince;
import com.apollographql.apollo.annotations.ApolloExperimental;
import com.apollographql.apollo.api.Adapter;
import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.CustomScalarType;
import com.apollographql.apollo.api.ExecutionContext;
import com.apollographql.apollo.api.ExecutionOptions;
import com.apollographql.apollo.api.MutableExecutionOptions;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo.api.http.HttpHeader;
import com.apollographql.apollo.api.http.HttpMethod;
import com.apollographql.apollo.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo.runtime.java.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.runtime.java.interceptor.internal.AutoPersistedQueryInterceptor;
import com.apollographql.apollo.runtime.java.interceptor.internal.DefaultInterceptorChain;
import com.apollographql.apollo.runtime.java.internal.DefaultApolloCall;
import com.apollographql.apollo.runtime.java.internal.DefaultApolloDisposable;
import com.apollographql.apollo.runtime.java.network.NetworkTransport;
import com.apollographql.apollo.runtime.java.network.http.HttpEngine;
import com.apollographql.apollo.runtime.java.network.http.HttpInterceptor;
import com.apollographql.apollo.runtime.java.network.http.HttpNetworkTransport;
import com.apollographql.apollo.runtime.java.network.http.internal.BatchingHttpInterceptor;
import com.apollographql.apollo.runtime.java.network.http.internal.OkHttpHttpEngine;
import com.apollographql.apollo.runtime.java.network.ws.WebSocketNetworkTransport;
import com.apollographql.apollo.runtime.java.network.ws.protocol.GraphQLWsProtocol;
import com.apollographql.apollo.runtime.java.network.ws.protocol.WsProtocol;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.apollographql.apollo.api.java.Assertions.checkNotNull;
import static java.util.concurrent.Executors.newCachedThreadPool;

/**
 * @deprecated The Java support has new maven coordinates at 'com.apollographql.java:client'. See <a href="https://go.apollo.dev/ak-moved-artifacts">the migration guide</a> for more details.
 */
@Deprecated
@ApolloDeprecatedSince(version = ApolloDeprecatedSince.Version.v4_0_0)
public class ApolloClient implements Closeable {
  private Executor executor;
  private List<ApolloInterceptor> interceptors;
  private CustomScalarAdapters customScalarAdapters;
  private NetworkInterceptor networkInterceptor;
  private HttpMethod httpMethod;
  private List<HttpHeader> httpHeaders;
  private Boolean sendApqExtensions;
  private Boolean sendDocument;
  private Boolean enableAutoPersistedQueries;
  private Boolean canBeBatched;

  private ApolloClient(
      Executor executor,
      NetworkTransport httpNetworkTransport,
      NetworkTransport subscriptionNetworkTransport,
      List<ApolloInterceptor> interceptors,
      CustomScalarAdapters customScalarAdapters,
      HttpMethod httpMethod,
      List<HttpHeader> httpHeaders,
      Boolean sendApqExtensions,
      Boolean sendDocument,
      Boolean enableAutoPersistedQueries,
      Boolean canBeBatched
  ) {
    this.executor = executor;
    this.interceptors = interceptors;
    this.customScalarAdapters = customScalarAdapters;
    this.httpMethod = httpMethod;
    this.httpHeaders = httpHeaders;
    this.sendApqExtensions = sendApqExtensions;
    this.sendDocument = sendDocument;
    this.enableAutoPersistedQueries = enableAutoPersistedQueries;
    this.canBeBatched = canBeBatched;

    networkInterceptor = new NetworkInterceptor(
        httpNetworkTransport,
        subscriptionNetworkTransport
    );
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

  public <D extends Operation.Data> ApolloDisposable execute(@NotNull ApolloRequest<D> apolloRequest, @NotNull ApolloCallback<D> callback) {
    ApolloRequest.Builder<D> requestBuilder = new ApolloRequest.Builder<>(apolloRequest.getOperation())
        .addExecutionContext(customScalarAdapters)
        .addExecutionContext(apolloRequest.getExecutionContext())
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries);
    if (apolloRequest.getHttpMethod() != null) {
      requestBuilder.httpMethod(apolloRequest.getHttpMethod());
    }
    if (apolloRequest.getHttpHeaders() != null) {
      requestBuilder.httpHeaders(apolloRequest.getHttpHeaders());
    }
    if (apolloRequest.getSendApqExtensions() != null) {
      requestBuilder.sendApqExtensions(apolloRequest.getSendApqExtensions());
    }
    if (apolloRequest.getSendDocument() != null) {
      requestBuilder.sendDocument(apolloRequest.getSendDocument());
    }
    if (apolloRequest.getEnableAutoPersistedQueries() != null) {
      requestBuilder.enableAutoPersistedQueries(apolloRequest.getEnableAutoPersistedQueries());
    }
    if (apolloRequest.getCanBeBatched() != null) {
      requestBuilder.addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, apolloRequest.getCanBeBatched().toString());
    } else if (canBeBatched != null)  {
      requestBuilder.addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, canBeBatched.toString());
    }
    DefaultApolloDisposable disposable = new DefaultApolloDisposable();
    ArrayList<ApolloInterceptor> interceptors = new ArrayList<>(this.interceptors);
    interceptors.add(networkInterceptor);
    executor.execute(() -> new DefaultInterceptorChain(interceptors, 0, disposable).proceed(requestBuilder.build(), callback));
    return disposable;
  }

  private static class NetworkInterceptor implements ApolloInterceptor {
    private NetworkTransport httpNetworkTransport;
    private NetworkTransport subscriptionNetworkTransport;

    private NetworkInterceptor(NetworkTransport httpNetworkTransport, NetworkTransport subscriptionNetworkTransport) {
      this.httpNetworkTransport = httpNetworkTransport;
      this.subscriptionNetworkTransport = subscriptionNetworkTransport;
    }

    @Override
    public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
      if (request.getOperation() instanceof Query || request.getOperation() instanceof Mutation) {
        httpNetworkTransport.execute(request, callback, chain.getDisposable());
      } else {
        subscriptionNetworkTransport.execute(request, callback, chain.getDisposable());
      }
    }
  }

  public CustomScalarAdapters getCustomScalarAdapters() {
    return customScalarAdapters;
  }

  public void close() {
    if (executor instanceof ApolloDefaultExecutor) {
      ((ApolloDefaultExecutor) executor).shutdown();
    }
  }


  public static class Builder implements MutableExecutionOptions<Builder> {
    private HttpEngine httpEngine;
    private NetworkTransport networkTransport;
    private NetworkTransport subscriptionNetworkTransport;
    private String httpServerUrl;
    private String webSocketServerUrl;
    private Call.Factory callFactory;
    private WebSocket.Factory webSocketFactory;
    private Executor executor;
    private List<ApolloInterceptor> interceptors = new ArrayList<>();
    private List<HttpInterceptor> httpInterceptors = new ArrayList<>();
    private WsProtocol.Factory wsProtocolFactory;
    private List<HttpHeader> wsHeaders = new ArrayList<>();
    private WebSocketNetworkTransport.ReopenWhen wsReopenWhen;
    private Long wsIdleTimeoutMillis;
    private final CustomScalarAdapters.Builder customScalarAdaptersBuilder = new CustomScalarAdapters.Builder();
    private ExecutionContext executionContext;
    private HttpMethod httpMethod;
    private final ArrayList<HttpHeader> httpHeaders = new ArrayList<>();
    private Boolean sendApqExtensions;
    private Boolean sendDocument;
    private Boolean enableAutoPersistedQueries;
    private Boolean canBeBatched;
    private Boolean httpExposeErrorBody;
    private Boolean retryOnError;

    public Builder() {
    }

    /**
     * The url of the GraphQL server used for HTTP. This is the same as {@link #httpServerUrl(String)}. See also
     * {@link #networkTransport(NetworkTransport)} for more customization
     */
    public Builder serverUrl(String serverUrl) {
      this.httpServerUrl = serverUrl;
      return this;
    }

    /**
     * The url of the GraphQL server used for HTTP. See also {@link #networkTransport(NetworkTransport)} for more customization
     */
    public Builder httpServerUrl(String httpServerUrl) {
      this.httpServerUrl = httpServerUrl;
      return this;
    }

    /**
     * The url of the GraphQL server used for WebSockets. See also {@link #subscriptionNetworkTransport(NetworkTransport)} for more
     * customization
     */
    public Builder webSocketServerUrl(String webSocketServerUrl) {
      this.webSocketServerUrl = webSocketServerUrl;
      return this;
    }

    /**
     * Set the {@link OkHttpClient} to use for making network requests.
     *
     * @param okHttpClient the client to use.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder okHttpClient(OkHttpClient okHttpClient) {
      this.callFactory = okHttpClient;
      this.webSocketFactory = okHttpClient;
      return this;
    }

    /**
     * Set the custom call factory for creating {@link Call} instances. <p> Note: Calling {@link #okHttpClient(OkHttpClient)} automatically
     * sets this value.
     */
    public Builder callFactory(Call.Factory factory) {
      this.callFactory = factory;
      return this;
    }

    /**
     * Set the custom call factory for creating {@link WebSocket} instances. <p> Note: Calling {@link #okHttpClient(OkHttpClient)}
     * automatically sets this value.
     */
    public Builder webSocketFactory(WebSocket.Factory factory) {
      this.webSocketFactory = factory;
      return this;
    }

    /**
     * The {@link Executor} to use for dispatching the requests.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder dispatcher(Executor dispatcher) {
      this.executor = dispatcher;
      return this;
    }

    public Builder addInterceptor(ApolloInterceptor interceptor) {
      this.interceptors.add(interceptor);
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

    public Builder addHttpInterceptor(@NotNull HttpInterceptor interceptor) {
      this.httpInterceptors.add(checkNotNull(interceptor, "interceptor is null"));
      return this;
    }

    public Builder addHttpInterceptors(@NotNull List<HttpInterceptor> interceptors) {
      this.httpInterceptors.addAll(checkNotNull(interceptors, "interceptors is null"));
      return this;
    }

    public Builder httpInterceptors(@NotNull List<HttpInterceptor> interceptors) {
      this.httpInterceptors = checkNotNull(interceptors, "interceptors is null");
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

    public Builder wsProtocolFactory(WsProtocol.Factory wsProtocolFactory) {
      this.wsProtocolFactory = wsProtocolFactory;
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

    public Builder wsReopenWhen(WebSocketNetworkTransport.ReopenWhen reopenWhen) {
      this.wsReopenWhen = reopenWhen;
      return this;
    }

    public Builder wsIdleTimeoutMillis(long wsIdleTimeoutMillis) {
      this.wsIdleTimeoutMillis = wsIdleTimeoutMillis;
      return this;
    }

    public Builder httpEngine(HttpEngine httpEngine) {
      this.httpEngine = httpEngine;
      return this;
    }

    public Builder httpExposeErrorBody(boolean httpExposeErrorBody) {
      this.httpExposeErrorBody = httpExposeErrorBody;
      return this;
    }

    public Builder networkTransport(NetworkTransport networkTransport) {
      this.networkTransport = networkTransport;
      return this;
    }

    public Builder subscriptionNetworkTransport(NetworkTransport subscriptionNetworkTransport) {
      this.subscriptionNetworkTransport = subscriptionNetworkTransport;
      return this;
    }

    @ApolloExperimental
    public Builder retryOnError(RetryOnError retryOnError) {
      throw new IllegalStateException("Not supported yet");
//      this.retryOnError = retryOnError;
//      return this;
    }


    public ApolloClient build() {
      if (executor == null) {
        executor = defaultExecutor();
      }

      NetworkTransport networkTransport;
      if (this.networkTransport != null) {
        if (httpServerUrl != null) throw new IllegalStateException("Apollo: 'httpServerUrl' has no effect if 'networkTransport' is set");
        if (httpEngine != null) throw new IllegalStateException("Apollo: 'httpEngine' has no effect if 'networkTransport' is set");
        if (callFactory != null) throw new IllegalStateException("Apollo: 'callFactory' has no effect if 'networkTransport' is set");
        if (httpExposeErrorBody != null)
          throw new IllegalStateException("Apollo: 'httpExposeErrorBody' has no effect if 'networkTransport' is set");
        networkTransport = this.networkTransport;
      } else {
        checkNotNull(httpServerUrl, "serverUrl is missing");
        if (callFactory != null) {
          if (httpEngine != null) {
            throw new IllegalStateException("Apollo: 'httpEngine' has no effect if 'callFactory' is set");
          }
        } else {
          callFactory = new OkHttpClient();
        }
        if (httpEngine == null) {
          httpEngine = new OkHttpHttpEngine(callFactory);
        }
        if (httpExposeErrorBody == null) {
          httpExposeErrorBody = false;
        }
        networkTransport = new HttpNetworkTransport(new DefaultHttpRequestComposer(httpServerUrl), httpEngine, httpInterceptors, httpExposeErrorBody);
      }

      NetworkTransport subscriptionNetworkTransport;
      if (this.subscriptionNetworkTransport != null) {
        if (webSocketServerUrl != null)
          throw new IllegalStateException("Apollo: 'webSocketServerUrl' has no effect if 'subscriptionNetworkTransport' is set");
        if (webSocketFactory != null)
          throw new IllegalStateException("Apollo: 'webSocketFactory' has no effect if 'subscriptionNetworkTransport' is set");
        if (wsProtocolFactory != null)
          throw new IllegalStateException("Apollo: 'wsProtocolFactory' has no effect if 'subscriptionNetworkTransport' is set");
        if (wsHeaders != null)
          throw new IllegalStateException("Apollo: 'wsHeaders' has no effect if 'subscriptionNetworkTransport' is set");
        if (wsIdleTimeoutMillis != null)
          throw new IllegalStateException("Apollo: 'wsIdleTimeoutMillis' has no effect if 'subscriptionNetworkTransport' is set");
        subscriptionNetworkTransport = this.subscriptionNetworkTransport;
      } else {
        if (webSocketServerUrl == null) {
          webSocketServerUrl = httpServerUrl;
        }
        if (webSocketServerUrl == null) {
          // Fallback to the regular NetworkTransport. This is unlikely to work but chances are
          // that the user is not going to use subscription, so it's better than failing
          subscriptionNetworkTransport = networkTransport;
        } else {
          if (webSocketFactory == null) {
            webSocketFactory = callFactory instanceof OkHttpClient ? (OkHttpClient) callFactory : new OkHttpClient();
          }
          if (wsProtocolFactory == null) {
            wsProtocolFactory = new GraphQLWsProtocol.Factory();
          }
          if (wsReopenWhen == null) {
            wsReopenWhen = (throwable, attempt) -> false;
          }
          if (wsIdleTimeoutMillis == null) {
            wsIdleTimeoutMillis = 60_000L;
          }
          subscriptionNetworkTransport = new WebSocketNetworkTransport(
              webSocketFactory,
              wsProtocolFactory,
              webSocketServerUrl,
              wsHeaders,
              wsReopenWhen,
              executor,
              wsIdleTimeoutMillis
          );
        }
      }

      return new ApolloClient(
          executor,
          networkTransport,
          subscriptionNetworkTransport,
          interceptors,
          customScalarAdaptersBuilder.build(),
          httpMethod,
          httpHeaders,
          sendApqExtensions,
          sendDocument,
          enableAutoPersistedQueries,
          canBeBatched
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

    /**
     * Batch HTTP queries to execute multiple at once. This reduces the number of HTTP round trips at the price of increased latency as
     * every request in the batch is now as slow as the slowest one. Some servers might have a per-HTTP-call cache making it faster to
     * resolve 1 big array of n queries compared to resolving the n queries separately.
     * <p>
     * See also {@link BatchingHttpInterceptor}.
     *
     * @param batchIntervalMillis the interval between two batches
     * @param maxBatchSize always send the batch when this threshold is reached
     */
    public Builder httpBatching(long batchIntervalMillis, int maxBatchSize, boolean enableByDefault) {
      addHttpInterceptor(new BatchingHttpInterceptor(batchIntervalMillis, maxBatchSize, httpExposeErrorBody != null && httpExposeErrorBody));
      canBeBatched(enableByDefault);
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
      this.canBeBatched = canBeBatched;
      return this;
    }
  }

  private static class ApolloDefaultExecutor implements Executor {
    private final ExecutorService executor = newCachedThreadPool(runnable -> new Thread(runnable, "Apollo Dispatcher"));

    @Override public void execute(@NotNull Runnable command) {
      executor.execute(command);
    }

    public void shutdown() {
      executor.shutdown();
    }
  }

  static private Executor defaultExecutor() {
    return new ApolloDefaultExecutor();
  }

  @ApolloExperimental
  interface RetryOnError {
    boolean retryOnError(ApolloRequest request);
  }
}
