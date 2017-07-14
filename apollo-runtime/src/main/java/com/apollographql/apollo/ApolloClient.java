package com.apollographql.apollo;

import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.http.HttpCacheStore;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.RecordFieldAdapter;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.internal.ApolloCallTracker;
import com.apollographql.apollo.internal.RealApolloCall;
import com.apollographql.apollo.internal.RealApolloPrefetch;
import com.apollographql.apollo.internal.ResponseFieldMapperFactory;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.cache.normalized.RealApolloStore;
import com.apollographql.apollo.internal.util.ApolloLogger;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
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
public final class ApolloClient implements ApolloQueryCall.Factory, ApolloMutationCall.Factory, ApolloPrefetch.Factory {

  public static Builder builder() {
    return new Builder();
  }

  private final HttpUrl serverUrl;
  private final Call.Factory httpCallFactory;
  private final HttpCache httpCache;
  private final ApolloStore apolloStore;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final Moshi moshi;
  private final ResponseFieldMapperFactory responseFieldMapperFactory = new ResponseFieldMapperFactory();
  private final ExecutorService dispatcher;
  private final HttpCachePolicy.Policy defaultHttpCachePolicy;
  private final ResponseFetcher defaultResponseFetcher;
  private final CacheHeaders defaultCacheHeaders;
  private final ApolloLogger logger;
  private final ApolloCallTracker tracker = new ApolloCallTracker();
  private final List<ApolloInterceptor> applicationInterceptors;
  private final boolean sendOperationIdentifiers;

  private ApolloClient(Builder builder) {
    this.serverUrl = builder.serverUrl;
    this.httpCallFactory = builder.okHttpClient;
    this.httpCache = builder.httpCache;
    this.apolloStore = builder.apolloStore;
    this.customTypeAdapters = builder.customTypeAdapters;
    this.moshi = builder.moshi;
    this.dispatcher = builder.dispatcher;
    this.defaultHttpCachePolicy = builder.defaultHttpCachePolicy;
    this.defaultCacheHeaders = builder.defaultCacheHeaders;
    this.defaultResponseFetcher = builder.defaultResponseFetcher;
    this.logger = builder.apolloLogger;
    this.applicationInterceptors = builder.applicationInterceptors;
    this.sendOperationIdentifiers = builder.sendOperationIdentifiers;
  }

  @Override
  public <D extends Mutation.Data, T, V extends Mutation.Variables> ApolloMutationCall<T> mutate(
      @Nonnull Mutation<D, T, V> mutation) {
    return newCall(mutation).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY);
  }

  @Override
  public <D extends Query.Data, T, V extends Query.Variables> ApolloQueryCall<T> query(@Nonnull Query<D, T, V> query) {
    return newCall(query);
  }

  /**
   * Prepares the {@link ApolloPrefetch} which will be executed at some point in the future.
   */
  @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloPrefetch prefetch(
      @Nonnull Operation<D, T, V> operation) {
    return new RealApolloPrefetch(operation, serverUrl, httpCallFactory,
        httpCache, moshi, dispatcher, logger, tracker, sendOperationIdentifiers);
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
   * Clear all entries from the normalized cache.
   */
  public void clearNormalizedCache() {
    apolloStore.clearAll();
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
      @Nonnull Operation<D, T, V> operation) {
    return RealApolloCall.<T>builder()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(defaultHttpCachePolicy)
        .moshi(moshi)
        .responseFieldMapperFactory(responseFieldMapperFactory)
        .customTypeAdapters(customTypeAdapters)
        .apolloStore(apolloStore)
        .responseFetcher(defaultResponseFetcher)
        .cacheHeaders(defaultCacheHeaders)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .tracker(tracker)
        .refetchQueries(Collections.<Query>emptyList())
        .refetchQueryNames(Collections.<OperationName>emptyList())
        .sendOperationIdentifiers(sendOperationIdentifiers)
        .build();
  }

  @SuppressWarnings("WeakerAccess") public static class Builder {
    OkHttpClient okHttpClient;
    HttpUrl serverUrl;
    HttpCacheStore httpCacheStore;
    ApolloStore apolloStore = ApolloStore.NO_APOLLO_STORE;
    Optional<NormalizedCacheFactory> cacheFactory = Optional.absent();
    Optional<CacheKeyResolver> cacheKeyResolver = Optional.absent();
    HttpCachePolicy.Policy defaultHttpCachePolicy = HttpCachePolicy.NETWORK_ONLY;
    ResponseFetcher defaultResponseFetcher = ApolloResponseFetchers.CACHE_FIRST;
    CacheHeaders defaultCacheHeaders = CacheHeaders.NONE;
    final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    private final Moshi.Builder moshiBuilder = new Moshi.Builder();
    Moshi moshi;
    ExecutorService dispatcher;
    Optional<Logger> logger = Optional.absent();
    HttpCache httpCache;
    ApolloLogger apolloLogger;
    final List<ApolloInterceptor> applicationInterceptors = new ArrayList<>();
    boolean sendOperationIdentifiers;

    private Builder() {
    }

    /**
     * Set the {@link OkHttpClient} to use for making network requests.
     *
     * @param okHttpClient the client to use.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder okHttpClient(@Nonnull OkHttpClient okHttpClient) {
      this.okHttpClient = checkNotNull(okHttpClient, "okHttpClient is null");
      return this;
    }

    /**
     * <p>Set the API server's base url.</p>
     *
     * @param serverUrl the url to set.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder serverUrl(@Nonnull HttpUrl serverUrl) {
      this.serverUrl = checkNotNull(serverUrl, "serverUrl is null");
      return this;
    }

    /**
     * <p>Set the API server's base url.</p>
     *
     * @param serverUrl the url to set.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder serverUrl(@Nonnull String serverUrl) {
      this.serverUrl = HttpUrl.parse(checkNotNull(serverUrl, "serverUrl == null"));
      return this;
    }

    /**
     * Set the configuration to be used for request/response http cache.
     *
     * @param cacheStore The store to use for reading and writing cached response.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder httpCacheStore(@Nonnull HttpCacheStore cacheStore) {
      this.httpCacheStore = checkNotNull(cacheStore, "cacheStore == null");
      return this;
    }

    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the {@link NormalizedCacheFactory} used to construct a {@link NormalizedCache}.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder normalizedCache(@Nonnull NormalizedCacheFactory normalizedCacheFactory) {
      return normalizedCache(normalizedCacheFactory, CacheKeyResolver.DEFAULT);
    }

    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the {@link NormalizedCacheFactory} used to construct a {@link NormalizedCache}.
     * @param keyResolver            the {@link CacheKeyResolver} to use to normalize records
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder normalizedCache(@Nonnull NormalizedCacheFactory normalizedCacheFactory,
        @Nonnull CacheKeyResolver keyResolver) {
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
    public <T> Builder addCustomTypeAdapter(@Nonnull ScalarType scalarType,
        @Nonnull final CustomTypeAdapter<T> customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      moshiBuilder.add(scalarType.javaType(), new JsonAdapter<T>() {
        @Override
        public T fromJson(com.squareup.moshi.JsonReader reader) throws IOException {
          return customTypeAdapter.decode(reader.nextString());
        }

        @Override
        public void toJson(JsonWriter writer, T value) throws IOException {
          //noinspection unchecked
          writer.value(customTypeAdapter.encode(value));
        }
      });
      return this;
    }

    /**
     * The #{@link ExecutorService} to use for dispatching the requests.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder dispatcher(@Nonnull ExecutorService dispatcher) {
      this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
      return this;
    }

    /**
     * Sets the http cache policy to be used as default for all GraphQL {@link Query} operations. Will be ignored for
     * any {@link Mutation} operations. By default http cache policy is set to {@link HttpCachePolicy#NETWORK_ONLY}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultHttpCachePolicy(@Nonnull HttpCachePolicy.Policy cachePolicy) {
      this.defaultHttpCachePolicy = checkNotNull(cachePolicy, "cachePolicy == null");
      return this;
    }

    /**
     * Set the default {@link CacheHeaders} strategy that will be passed
     * to the {@link com.apollographql.apollo.interceptor.FetchOptions} used in each new {@link ApolloCall}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultCacheHeaders(@Nonnull CacheHeaders cacheHeaders) {
      this.defaultCacheHeaders = checkNotNull(cacheHeaders, "cacheHeaders == null");
      return this;
    }

    /**
     * Set the default {@link ResponseFetcher} to be used with each new {@link ApolloCall}.
     *
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder defaultResponseFetcher(@Nonnull ResponseFetcher defaultResponseFetcher) {
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
    public Builder addApplicationInterceptor(@Nonnull ApolloInterceptor interceptor) {
      applicationInterceptors.add(interceptor);
      return this;
    }

    /**
     *
     * @param sendOperationIdentifiers True if ApolloClient should send a operation identifier instead of the operation
     *                        definition. Default: false.
     * @return The {@link Builder} object to be used for chaining method calls
     */
    public Builder sendOperationIdentifiers(boolean sendOperationIdentifiers) {
      this.sendOperationIdentifiers = sendOperationIdentifiers;
      return this;
    }

    /**
     * Builds the {@link ApolloClient} instance using the configured values.
     *
     * Note that if the {@link #dispatcher} is not called, then a default {@link ExecutorService} is used.
     *
     * @return The configured {@link ApolloClient}
     */
    public ApolloClient build() {
      checkNotNull(okHttpClient, "okHttpClient is null");
      checkNotNull(serverUrl, "serverUrl is null");

      apolloLogger = new ApolloLogger(logger);
      moshi = moshiBuilder.build();

      if (httpCacheStore != null) {
        httpCache = new HttpCache(httpCacheStore, apolloLogger);
        okHttpClient = okHttpClient.newBuilder().addInterceptor(httpCache.interceptor()).build();
      }

      if (cacheFactory.isPresent() && cacheKeyResolver.isPresent()) {
        final NormalizedCache normalizedCache =
            cacheFactory.get().createNormalizedCache(RecordFieldAdapter.create(moshi));
        this.apolloStore = new RealApolloStore(normalizedCache, cacheKeyResolver.get(), customTypeAdapters,
            apolloLogger);
      }

      if (dispatcher == null) {
        dispatcher = defaultDispatcher();
      }

      return new ApolloClient(this);
    }

    private ExecutorService defaultDispatcher() {
      return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), new ThreadFactory() {
        @Override public Thread newThread(Runnable runnable) {
          return new Thread(runnable, "Apollo Dispatcher");
        }
      });
    }
  }
}
