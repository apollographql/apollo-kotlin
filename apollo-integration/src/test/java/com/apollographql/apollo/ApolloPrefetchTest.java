package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.cache.http.HttpCache;
import com.apollographql.apollo.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo.cache.http.ApolloHttpCache;
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Predicate;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.internal.io.FileSystem;
import okhttp3.internal.io.InMemoryFileSystem;
import okhttp3.mockwebserver.MockWebServer;

import static com.apollographql.apollo.Utils.TIME_OUT_SECONDS;
import static com.apollographql.apollo.Utils.assertResponse;
import static com.apollographql.apollo.Utils.enqueueAndAssertResponse;
import static com.apollographql.apollo.Utils.mockResponse;
import static com.google.common.truth.Truth.assertThat;

public class ApolloPrefetchTest {
  private ApolloClient apolloClient;
  private MockWebServer server;
  @Rule public InMemoryFileSystem inMemoryFileSystem = new InMemoryFileSystem();
  okhttp3.Request lastHttRequest;
  okhttp3.Response lastHttResponse;
  private MockHttpCacheStore cacheStore;
  private OkHttpClient okHttpClient;

  @Before
  public void setup() {
    server = new MockWebServer();
    cacheStore = new MockHttpCacheStore();
    cacheStore.delegate = new DiskLruHttpCacheStore(inMemoryFileSystem, new File("/cache/"), Integer.MAX_VALUE);
    okHttpClient = new OkHttpClient.Builder()
        .addInterceptor(new Interceptor() {
          @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            lastHttRequest = chain.request();
            lastHttResponse = chain.proceed(lastHttRequest);
            return lastHttResponse;
          }
        })
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .httpCache(new ApolloHttpCache(cacheStore, null))
        .build();
  }

  @After public void tearDown() {
    try {
      apolloClient.clearHttpCache();
      server.shutdown();
    } catch (Exception ignore) {
    }
  }

  @Test public void prefetchDefault() throws IOException, ApolloException {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));
    awaitDone(apolloClient.prefetch(new AllPlanetsQuery()));
    checkCachedResponse("HttpCacheTestAllPlanets.json");
    assertResponse(
        apolloClient
            .query(new AllPlanetsQuery())
            .httpCachePolicy(HttpCachePolicy.CACHE_ONLY.expireAfter(2, TimeUnit.SECONDS)),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> dataResponse) throws Exception {
            return !dataResponse.hasErrors();
          }
        }
    );
  }

  @Test public void prefetchNoCacheStore() throws Exception {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .build();

    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));
    awaitDone(apolloClient.prefetch(new AllPlanetsQuery()));

    enqueueAndAssertResponse(
        server,
        "HttpCacheTestAllPlanets.json",
        apolloClient.query(new AllPlanetsQuery()),
        new Predicate<Response<AllPlanetsQuery.Data>>() {
          @Override public boolean test(Response<AllPlanetsQuery.Data> response) throws Exception {
            return !response.hasErrors();
          }
        }
    );
  }

  @Test public void prefetchFileSystemWriteFailure() throws IOException {
    FaultyHttpCacheStore faultyCacheStore = new FaultyHttpCacheStore(FileSystem.SYSTEM);
    cacheStore.delegate = faultyCacheStore;

    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_HEADER_WRITE);
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));

    Rx2Apollo.from(apolloClient.prefetch(new AllPlanetsQuery()))
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertError(Exception.class);
    checkNoCachedResponse();

    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"));
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_BODY_WRITE);
    Rx2Apollo.from(apolloClient.prefetch(new AllPlanetsQuery()))
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS)
        .assertError(Exception.class);
    checkNoCachedResponse();
  }

  private void checkCachedResponse(String fileName) throws IOException {
    String cacheKey = ApolloServerInterceptor.cacheKey(lastHttRequest.body());
    okhttp3.Response response = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(response).isNotNull();
    assertThat(response.body().source().readUtf8()).isEqualTo(Utils.readFileToString(getClass(), "/" + fileName));
    response.body().source().close();
  }

  private void checkNoCachedResponse() throws IOException {
    String cacheKey = lastHttRequest.header(HttpCache.CACHE_KEY_HEADER);
    okhttp3.Response cachedResponse = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(cachedResponse).isNull();
  }

  private static void awaitDone(ApolloPrefetch prefetch) {
    Rx2Apollo.from(prefetch)
        .test()
        .awaitDone(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }
}
