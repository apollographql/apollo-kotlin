package com.apollographql.apollo3.java;

import com.apollographql.apollo3.api.Adapter;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.CustomScalarType;
import com.apollographql.apollo3.api.ExecutionContext;
import com.apollographql.apollo3.api.ExecutionOptions;
import com.apollographql.apollo3.api.MutableExecutionOptions;
import com.apollographql.apollo3.api.Mutation;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.api.Subscription;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.cache.normalized.NormalizedCache;
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator;
import com.apollographql.apollo3.cache.normalized.api.CacheResolver;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory;
import com.apollographql.apollo3.java.cache.normalized.ApolloStore;
import com.apollographql.apollo3.java.cache.normalized.internal.ApolloStoreAdapter;
import com.apollographql.apollo3.java.internal.ApolloClientAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public class ApolloClient implements ExecutionOptions, Closeable {
  private final com.apollographql.apollo3.ApolloClient wrappedApolloClient;

  private ApolloClient(com.apollographql.apollo3.ApolloClient wrappedApolloClient) {
    this.wrappedApolloClient = wrappedApolloClient;
  }

  public <D extends Query.Data> ApolloCall<D> query(@NotNull Query<D> query) {
    return new ApolloCall(wrappedApolloClient.query(query));
  }

  public <D extends Mutation.Data> ApolloCall<D> mutation(@NotNull Mutation<D> mutation) {
    return new ApolloCall(wrappedApolloClient.mutation(mutation));
  }

  public <D extends Subscription.Data> ApolloCall<D> subscription(@NotNull Subscription<D> subscription) {
    return new ApolloCall(wrappedApolloClient.subscription(subscription));
  }

  @NotNull @Override public ExecutionContext getExecutionContext() {
    return wrappedApolloClient.getExecutionContext();
  }

  @Nullable @Override public HttpMethod getHttpMethod() {
    return wrappedApolloClient.getHttpMethod();
  }

  @Nullable @Override public List<HttpHeader> getHttpHeaders() {
    return wrappedApolloClient.getHttpHeaders();
  }

  @Nullable @Override public Boolean getSendApqExtensions() {
    return wrappedApolloClient.getSendApqExtensions();
  }

  @Nullable @Override public Boolean getSendDocument() {
    return wrappedApolloClient.getSendDocument();
  }

  @Nullable @Override public Boolean getEnableAutoPersistedQueries() {
    return wrappedApolloClient.getEnableAutoPersistedQueries();
  }

  @Nullable @Override public Boolean getCanBeBatched() {
    return wrappedApolloClient.getCanBeBatched();
  }

  @Override public void close() throws IOException {
    wrappedApolloClient.close();
  }

  @NotNull
  public ApolloStore getApolloStore() {
    return new ApolloStoreAdapter(NormalizedCache.getApolloStore(wrappedApolloClient));
  }

  public static class Builder implements MutableExecutionOptions<Builder> {
    private com.apollographql.apollo3.ApolloClient.Builder builder = new com.apollographql.apollo3.ApolloClient.Builder();

    public ApolloClient build() {
      return new ApolloClient(builder.build());
    }

    @NotNull @Override public ExecutionContext getExecutionContext() {
      return builder.getExecutionContext();
    }

    @Nullable @Override public HttpMethod getHttpMethod() {
      return builder.getHttpMethod();
    }

    @Nullable @Override public List<HttpHeader> getHttpHeaders() {
      return builder.getHttpHeaders();
    }

    @Nullable @Override public Boolean getSendApqExtensions() {
      return builder.getSendApqExtensions();
    }

    @Nullable @Override public Boolean getSendDocument() {
      return builder.getSendDocument();
    }

    @Nullable @Override public Boolean getEnableAutoPersistedQueries() {
      return builder.getEnableAutoPersistedQueries();
    }

    @Nullable @Override public Boolean getCanBeBatched() {
      return builder.getCanBeBatched();
    }

    @Override public Builder addExecutionContext(@NotNull ExecutionContext executionContext) {
      builder.addExecutionContext(executionContext);
      return this;
    }

    @Override public Builder httpMethod(@Nullable HttpMethod httpMethod) {
      builder.httpMethod(httpMethod);
      return this;
    }

    @Override public Builder httpHeaders(@Nullable List<HttpHeader> httpHeaders) {
      builder.httpHeaders(httpHeaders);
      return this;
    }

    @Override public Builder addHttpHeader(@NotNull String name, @NotNull String value) {
      builder.addHttpHeader(name, value);
      return this;
    }

    @Override public Builder sendApqExtensions(@Nullable Boolean sendApqExtensions) {
      builder.sendApqExtensions(sendApqExtensions);
      return this;
    }

    @Override public Builder sendDocument(@Nullable Boolean sendDocument) {
      builder.sendDocument(sendDocument);
      return this;
    }

    @Override public Builder enableAutoPersistedQueries(@Nullable Boolean enableAutoPersistedQueries) {
      builder.enableAutoPersistedQueries(enableAutoPersistedQueries);
      return this;
    }

    @Override public Builder canBeBatched(@Nullable Boolean canBeBatched) {
      builder.canBeBatched(canBeBatched);
      return this;
    }

    public Builder serverUrl(@NotNull String url) {
      builder.serverUrl(url);
      return this;
    }

    // TODO
//    public Builder httpEngine(HttpEngine httpEngine) {}

    public Builder httpExposeErrorBody(boolean httpExposeErrorBody) {
      builder.httpExposeErrorBody(httpExposeErrorBody);
      return this;
    }

    // TODO
//    public Builder addHttpInterceptor(HttpInterceptor httpInterceptor) {}

    public Builder webSocketServerUrl(@NotNull String webSocketServerUrl) {
      builder.webSocketServerUrl(webSocketServerUrl);
      return this;
    }

    public Builder webSocketIdleTimeoutMillis(long webSocketIdleTimeoutMillis) {
      builder.webSocketIdleTimeoutMillis(webSocketIdleTimeoutMillis);
      return this;
    }

    // TODO
//    public Builder wsProtocol(@NotNull WsProtocol.Factory wsProtocolFactory) {}

    // TODO
//    public Builder webSocketEngine(@NotNull WebSocketEngine webSocketEngine) {}

    public Builder webSocketReopenWhen(@NotNull RetryPredicate reopenWhen) {
      ApolloClientAdapter.webSocketReopenWhen(builder, reopenWhen);
      return this;
    }

    // TODO
//    public Builder networkTransport(@NotNull NetworkTransport networkTransport) {}

    // TODO
//    public Builder subscriptionNetworkTransport(@NotNull NetworkTransport subscriptionNetworkTransport) {}

    public Builder customScalarAdapters(@NotNull CustomScalarAdapters customScalarAdapters) {
      builder.customScalarAdapters(customScalarAdapters);
      return this;
    }

    public <T> Builder addCustomScalarAdapter(@NotNull CustomScalarType customScalarType, @NotNull Adapter<T> customScalarAdapter) {
      builder.addCustomScalarAdapter(customScalarType, customScalarAdapter);
      return this;
    }

    // TODO
//    public Builder addInterceptor(@NotNull ApolloInterceptor interceptor) {}

    // TODO
//    public Builder addInterceptors(@NotNull List<ApolloInterceptor> interceptors) {}

    // TODO
//    public Builder interceptors(@NotNull List<ApolloInterceptor> interceptors) {}

    public Builder autoPersistedQueries(
        @NotNull HttpMethod httpMethodForHashedQueries,
        @NotNull HttpMethod httpMethodForDocumentQueries,
        boolean enableByDefault
    ) {
      builder.autoPersistedQueries(httpMethodForHashedQueries, httpMethodForDocumentQueries, enableByDefault);
      return this;
    }

    public Builder autoPersistedQueries() {
      builder.autoPersistedQueries();
      return this;
    }

    public Builder httpBatching(long batchIntervalMillis, int maxBatchSize, boolean enableByDefault) {
      builder.httpBatching(batchIntervalMillis, maxBatchSize, enableByDefault);
      return this;
    }

    public Builder httpBatching() {
      builder.httpBatching();
      return this;
    }

    public Builder normalizedCache(
        @NotNull NormalizedCacheFactory normalizedCacheFactory,
        @NotNull CacheKeyGenerator cacheKeyGenerator,
        @NotNull CacheResolver cacheResolver,
        boolean writeToCacheAsynchronously
    ) {
      NormalizedCache.configureApolloClientBuilder(builder, normalizedCacheFactory, cacheKeyGenerator, cacheResolver, writeToCacheAsynchronously);
      return this;
    }

    public Builder normalizedCache(@NotNull NormalizedCacheFactory normalizedCacheFactory) {
      NormalizedCache.configureApolloClientBuilder(builder, normalizedCacheFactory);
      return this;
    }

    // TODO
//    public Builder store(@NotNull ApolloStore store, boolean writeToCacheAsynchronously) {}
  }

}
