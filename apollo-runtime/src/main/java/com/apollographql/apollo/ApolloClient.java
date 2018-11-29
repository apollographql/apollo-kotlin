package com.apollographql.apollo;

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.internal.ApolloCallTracker;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.internal.RealApolloCall;
import com.apollographql.apollo.internal.RealApolloPrefetch;
import com.apollographql.apollo.internal.RealApolloSubscriptionCall;
import com.apollographql.apollo.internal.ResponseFieldMapperFactory;
import com.apollographql.apollo.internal.cache.normalized.RealApolloStore;
import com.apollographql.apollo.internal.subscription.NoOpSubscriptionManager;
import com.apollographql.apollo.internal.subscription.RealSubscriptionManager;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.ScalarTypeAdapters;
import com.apollographql.apollo.subscription.SubscriptionTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloClient class represents the abstraction for the graphQL client that will be used to execute queries and read
 * the responses back.
 *
 * <h3>ApolloClient should be shared</h3>
 *
 * Since each ApolloClient holds its own connection pool and thread pool, it is recommended to only create a single
 * ApolloClient and use that for execution of all the queries, as this would reduce latency and would also save memory.
 * Conversely, creating a client for each query execution would result in resource wastage on idle pools.
 *
 *
 * <p>See the {@link ApolloClient.Builder} class for configuring the ApolloClient.
 */
public final class ApolloClient implements ApolloQueryCall.Factory, ApolloMutationCall.Factory, ApolloPrefetch.Factory,
    ApolloSubscriptionCall.Factory {

  public static Builder builder() {
    return new Builder();
  }

  private final HttpUrl serverUrl;
  private final Call.Factory httpCallFactory;
  private final HttpCache httpCache;
  private final ApolloStore apolloStore;
  private final ScalarTypeAdapters scalarTypeAdapters;
  private final ResponseFieldMapperFactory responseFieldMapperFactory = new ResponseFieldMapperFactory();
  private final Executor dispatcher;
  private final HttpCachePolicy.Policy defaultHttpCachePolicy;
  private final ResponseFetcher defaultResponseFetcher;
  private final CacheHeaders defaultCacheHeaders;
  private final ApolloLogger logger;
  private final ApolloCallTracker tracker = new ApolloCallTracker();
  private final List<ApolloInterceptor> applicationInterceptors;
  private final boolean enableAutoPersistedQueries;
  private final SubscriptionManager subscriptionManager;

  ApolloClient(HttpUrl serverUrl,
      Call.Factory httpCallFactory,
      HttpCache httpCache,
      ApolloStore apolloStore,
      ScalarTypeAdapters scalarTypeAdapters,
      Executor dispatcher,
      HttpCachePolicy.Policy defaultHttpCachePolicy,
      ResponseFetcher defaultResponseFetcher,
      CacheHeaders defaultCacheHeaders,
      ApolloLogger logger,
      List<ApolloInterceptor> applicationInterceptors,
      boolean enableAutoPersistedQueries,
      SubscriptionManager subscriptionManager) {
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.httpCache = httpCache;
    this.apolloStore = apolloStore;
    this.scalarTypeAdapters = scalarTypeAdapters;
    this.dispatcher = dispatcher;
    this.defaultHttpCachePolicy = defaultHttpCachePolicy;
    this.defaultResponseFetcher = defaultResponseFetcher;
    this.defaultCacheHeaders = defaultCacheHeaders;
    this.logger = logger;
    this.applicationInterceptors = applicationInterceptors;
    this.enableAutoPersistedQueries = enableAutoPersistedQueries;
    this.subscriptionManager = subscriptionManager;
  }

  @Override
  public <D extends Mutation.Data, T, V extends Mutation.Variables> ApolloMutationCall<T> mutate(
      @NotNull Mutation<D, T, V> mutation) {
    return newCall(mutation).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY);
  }

  @Override
  public <D extends Mutation.Data, T, V extends Mutation.Variables> ApolloMutationCall<T> mutate(
      @NotNull Mutation<D, T, V> mutation, @NotNull D withOptimisticUpdates) {
    checkNotNull(withOptimisticUpdates, "withOptimisticUpdate == null");
    return newCall(mutation).toBuilder().responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .optimisticUpdates(Optional.<Operation.Data>fromNullable(withOptimisticUpdates)).build();
  }

  @Override
  public <D extends Query.Data, T, V extends Query.Variables> ApolloQueryCall<T> query(@NotNull Query<D, T, V> query) {
    return newCall(query);
  }

  @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloPrefetch prefetch(
      @NotNull Operation<D, T, V> operation) {
    return new RealApolloPrefetch(operation, serverUrl, httpCallFactory, scalarTypeAdapters, dispatcher, logger,
        tracker);
  }

  @Override
  public <D extends Subscription.Data, T, V extends Subscription.Variables> ApolloSubscriptionCall<T> subscribe(
      @NotNull Subscription<D, T, V> subscription) {
    return new RealApolloSubscriptionCall<>(subscription, subscriptionManager);
  }

  /**
   * @return The default {@link CacheHeaders} which this instance of {@link ApolloClient} was configured.
   */
  public CacheHeaders defaultCacheHeaders() {
    return defaultCacheHeaders;
  }

  /**
   * Clear all entries from the {@link HttpCache}, if present.
   */
  public void clearHttpCache() {
    if (httpCache != null) {
      httpCache.clear();
    }
  }

  /**
   * Clear all entries from the normalized cache. This is asynchronous operation and will be scheduled on the
   * dispatcher
   *
   * @param callback to be notified when operation is completed
   */
  public void clearNormalizedCache(@NotNull ApolloStoreOperation.Callback<Boolean> callback) {
    checkNotNull(callback, "callback == null");
    apolloStore.clearAll().enqueue(callback);
  }

  /**
   * Clear all entries from the normalized cache. This is synchronous operation and will be executed int the current
   * thread
   *
   * @return {@code true} if operation succeed, {@code false} otherwise
   */
  public boolean clearNormalizedCache() {
    return apolloStore.clearAll().execute();
  }

  /**
   * @return The {@link ApolloStore} managing access to the normalized cache created by {@link
   * Builder#normalizedCache(NormalizedCacheFactory, CacheKeyResolver)}  }
   */
  public ApolloStore apolloStore() {
    return apolloStore;
  }

  /**
   * Sets the idleResourceCallback which will be called when this ApolloClient is idle.
   */
  public void idleCallback(IdleResourceCallback idleResourceCallback) {
    tracker.setIdleResourceCallback(idleResourceCallback);
  }

  /**
   * Returns the count of {@link ApolloCall} & {@link ApolloPrefetch} objects which are currently in progress.
   */
  public int activeCallsCount() {
    return tracker.activeCallsCount();
  }

  Response cachedHttpResponse(String cacheKey) throws IOException {
    if (httpCache != null) {
      return httpCache.read(cacheKey);
    } else {
      return null;
    }
  }

  private <D extends Operation.Data, T, V extends Operation.Variables> RealApolloCall<T> newCall(
      @NotNull Operation<D, T, V> operation) {
    return RealApolloCall.<T>builder()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(defaultHttpCachePolicy)
        .responseFieldMapperFactory(responseFieldMapperFactory)
        .scalarTypeAdapters(scalarTypeAdapters)
        .apolloStore(apolloStore)
        .responseFetcher(defaultResponseFetcher)
        .cacheHeaders(defaultCacheHeaders)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .tracker(tracker)
        .refetchQueries(Collections.<Query>emptyList())
        .refetchQueryNames(Collections.<OperationName>emptyList())
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .build();
  }

  @SuppressWarnings("WeakerAccess") public static class Builder {
    Call.Factory callFactory;
    HttpUrl serverUrl;
    HttpCache httpCache;
    ApolloStore apolloStore = ApolloStore.NO_APOLLO_STORE;
    Optional<NormalizedCacheFactory> cacheFactory = Optional.absent();
    Optional<CacheKeyResolver> cacheKeyResolver = Optional.absent();
    HttpCachePolicy.Policy defaultHttpCachePolicy = HttpCachePolicy.NETWORK_ONLY;
    ResponseFetcher defaultResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
    CacheHeaders defaultCacheHeaders = CacheHeaders.NONE;
    final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    Executor dispatcher;
    Optional<Logger> logger = Optional.absent();
    final List<ApolloInterceptor> applicationInterceptors = new ArrayList<>();
    boolean enableAutoPersistedQueries;
    Optional<SubscriptionTransport.Factory> subscriptionTransportFactory = Optional.absent();
    Optional<Map<String, Object>> subscriptionConnectionParams = Optional.absent();
    long subscriptionHeartbeatTimeout = -1;

    Builder() {
    }

    /**
     * Set the {@link OkHttpClient} to use for making network requests.
     *
     * @param okHttpClient the client to use.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder okHttpClient(@NotNull OkHttpClient okHttpClient) {
      return callFactory(checkNotNull(okHttpClient, "okHttpClient is null"));
    }

    /**
     * Set the custom call factory for creating {@link Call} instances. <p> Note: Calling {@link
     * #okHttpClient(OkHttpClient)} automatically sets this value.
     */
    public Builder callFactory(@NotNull Call.Factory factory) {
      this.callFactory = checkNotNull(factory, "factory == null");
      return this;
    }

    /**
     * <p>Set the API server's base url.</p>
     *
     * @param serverUrl the url to set.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder serverUrl(@NotNull HttpUrl serverUrl) {
      this.serverUrl = checkNotNull(serverUrl, "serverUrl is null");
      return this;
    }

    /**
     * <p>Set the API server's base url.</p>
     *
     * @param serverUrl the url to set.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder serverUrl(@NotNull String serverUrl) {
      this.serverUrl = HttpUrl.parse(checkNotNull(serverUrl, "serverUrl == null"));
      return this;
    }

    /**
     * Set the configuration to be used for request/response http cache.
     *
     * @param httpCache The to use for reading and writing cached response.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder httpCache(@NotNull HttpCache httpCache) {
      this.httpCache = checkNotNull(httpCache, "httpCache == null");
      return this;
    }

    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the {@link NormalizedCacheFactory} used to construct a {@link NormalizedCache}.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder normalizedCache(@NotNull NormalizedCacheFactory normalizedCacheFactory) {
      return normalizedCache(normalizedCacheFactory, CacheKeyResolver.DEFAULT);
    }

    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the {@link NormalizedCacheFactory} used to construct a {@link NormalizedCache}.
     * @param keyResolver            the {@link CacheKeyResolver} to use to normalize records
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder normalizedCache(@NotNull NormalizedCacheFactory normalizedCacheFactory,
        @NotNull CacheKeyResolver keyResolver) {
      cacheFactory = Optional.fromNullable(checkNotNull(normalizedCacheFactory, "normalizedCacheFactory == null"));
      cacheKeyResolver = Optional.fromNullable(checkNotNull(keyResolver, "cacheKeyResolver == null"));
      return this;
    }

    /**
     * Set the type adapter to use for serializing and de-serializing custom GraphQL scalar types.
     *
     * @param scalarType        the scalar type to serialize/deserialize
     * @param customTypeAdapter the type adapter to use
     * @param <T>               the value type
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public <T> Builder addCustomTypeAdapter(@NotNull ScalarType scalarType,
        @NotNull final CustomTypeAdapter<T> customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      return this;
    }

    /**
     * The #{@link Executor} to use for dispatching the requests.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder dispatcher(@NotNull Executor dispatcher) {
      this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
      return this;
    }

    /**
     * Sets the http cache policy to be used as default for all GraphQL {@link Query} operations. Will be ignored for
     * any {@link Mutation} operations. By default http cache policy is set to {@link HttpCachePolicy#NETWORK_ONLY}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultHttpCachePolicy(@NotNull HttpCachePolicy.Policy cachePolicy) {
      this.defaultHttpCachePolicy = checkNotNull(cachePolicy, "cachePolicy == null");
      return this;
    }

    /**
     * Set the default {@link CacheHeaders} strategy that will be passed to the {@link
     * com.apollographql.apollo.interceptor.FetchOptions} used in each new {@link ApolloCall}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultCacheHeaders(@NotNull CacheHeaders cacheHeaders) {
      this.defaultCacheHeaders = checkNotNull(cacheHeaders, "cacheHeaders == null");
      return this;
    }

    /**
     * Set the default {@link ResponseFetcher} to be used with each new {@link ApolloCall}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultResponseFetcher(@NotNull ResponseFetcher defaultResponseFetcher) {
      this.defaultResponseFetcher = checkNotNull(defaultResponseFetcher, "defaultResponseFetcher == null");
      return this;
    }

    /**
     * The {@link Logger} to use for logging purposes.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder logger(@Nullable Logger logger) {
      this.logger = Optional.fromNullable(logger);
      return this;
    }

    /**
     * <p>Adds an interceptor that observes the full span of each call: from before the connection is established until
     * after the response source is selected (either the server, cache or both). This method can be called multiple
     * times for adding multiple application interceptors. </p>
     *
     * <p>Note: Interceptors will be called <b>in the order in which they are added to the list of interceptors</b> and
     * if any of the interceptors tries to short circuit the responses, then subsequent interceptors <b>won't</b> be
     * called.</p>
     *
     * @param interceptor Application level interceptor to add
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder addApplicationInterceptor(@NotNull ApolloInterceptor interceptor) {
      applicationInterceptors.add(interceptor);
      return this;
    }

    /**
     * @param enableAutoPersistedQueries True if ApolloClient should enable Automatic Persisted Queries support.
     *                                   Default: false.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder enableAutoPersistedQueries(boolean enableAutoPersistedQueries) {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries;
      return this;
    }

    /**
     * <p>Sets up subscription transport factory to be used for subscription server communication.<p/> See also: {@link
     * com.apollographql.apollo.subscription.WebSocketSubscriptionTransport}
     *
     * @param subscriptionTransportFactory transport layer to be used for subscriptions.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder subscriptionTransportFactory(@NotNull SubscriptionTransport.Factory subscriptionTransportFactory) {
      this.subscriptionTransportFactory = Optional.of(checkNotNull(subscriptionTransportFactory,
          "subscriptionTransportFactory is null"));
      return this;
    }

    /**
     * <p>Sets up subscription connection parameters to be sent to the server when connection is established with
     * subscription server</p>
     *
     * @param subscriptionConnectionParams map of connection parameters to be sent
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder subscriptionConnectionParams(@NotNull Map<String, Object> subscriptionConnectionParams) {
      this.subscriptionConnectionParams = Optional.of(checkNotNull(subscriptionConnectionParams,
          "subscriptionConnectionParams is null"));
      return this;
    }

    /**
     * <p>Sets up subscription heartbeat message timeout. Timeout for how long subscription manager should wait for a
     * keep-alive message from the subscription server before reconnect. <b>NOTE: will be ignored if server doesn't send
     * keep-alive messages.<b/></p>. By default heartbeat timeout is disabled.
     *
     * @param timeout  connection keep alive timeout. Min value is 10 secs.
     * @param timeUnit time unit
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder subscriptionHeartbeatTimeout(long timeout, @NotNull TimeUnit timeUnit) {
      checkNotNull(timeUnit, "timeUnit is null");
      this.subscriptionHeartbeatTimeout = Math.max(timeUnit.toMillis(timeout), TimeUnit.SECONDS.toMillis(10));
      return this;
    }

    /**
     * Builds the {@link ApolloClient} instance using the configured values.
     *
     * Note that if the {@link #dispatcher} is not called, then a default {@link Executor} is used.
     *
     * @return The configured {@link ApolloClient}
     */
    public ApolloClient build() {
      checkNotNull(serverUrl, "serverUrl is null");

      ApolloLogger apolloLogger = new ApolloLogger(logger);

      okhttp3.Call.Factory callFactory = this.callFactory;
      if (callFactory == null) {
        callFactory = new OkHttpClient();
      }

      HttpCache httpCache = this.httpCache;
      if (httpCache != null) {
        callFactory = addHttpCacheInterceptorIfNeeded(callFactory, httpCache.interceptor());
      }

      Executor dispatcher = this.dispatcher;
      if (dispatcher == null) {
        dispatcher = defaultDispatcher();
      }

      ScalarTypeAdapters scalarTypeAdapters = new ScalarTypeAdapters(customTypeAdapters);

      ApolloStore apolloStore = this.apolloStore;
      Optional<NormalizedCacheFactory> cacheFactory = this.cacheFactory;
      Optional<CacheKeyResolver> cacheKeyResolver = this.cacheKeyResolver;
      if (cacheFactory.isPresent() && cacheKeyResolver.isPresent()) {
        final NormalizedCache normalizedCache = cacheFactory.get().createChain(RecordFieldJsonAdapter.create());
        apolloStore = new RealApolloStore(normalizedCache, cacheKeyResolver.get(), scalarTypeAdapters, dispatcher,
            apolloLogger);
      }

      SubscriptionManager subscriptionManager = new NoOpSubscriptionManager();
      Optional<SubscriptionTransport.Factory> subscriptionTransportFactory = this.subscriptionTransportFactory;
      if (subscriptionTransportFactory.isPresent()) {
        subscriptionManager = new RealSubscriptionManager(scalarTypeAdapters, subscriptionTransportFactory.get(),
            subscriptionConnectionParams.or(Collections.<String, Object>emptyMap()), dispatcher,
            subscriptionHeartbeatTimeout);
      }

      return new ApolloClient(serverUrl,
          callFactory,
          httpCache,
          apolloStore,
          scalarTypeAdapters,
          dispatcher,
          defaultHttpCachePolicy,
          defaultResponseFetcher,
          defaultCacheHeaders,
          apolloLogger,
          applicationInterceptors,
          enableAutoPersistedQueries,
          subscriptionManager);
    }

    private Executor defaultDispatcher() {
      return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), new ThreadFactory() {
        @Override public Thread newThread(@NotNull Runnable runnable) {
          return new Thread(runnable, "Apollo Dispatcher");
        }
      });
    }

    private static okhttp3.Call.Factory addHttpCacheInterceptorIfNeeded(Call.Factory callFactory,
        Interceptor httpCacheInterceptor) {
      if (callFactory instanceof OkHttpClient) {
        OkHttpClient client = (OkHttpClient) callFactory;
        for (Interceptor interceptor : client.interceptors()) {
          if (interceptor.getClass().equals(httpCacheInterceptor.getClass())) {
            return callFactory;
          }
        }
        return client.newBuilder().addInterceptor(httpCacheInterceptor).build();
      }
      return callFactory;
    }
  }
}
