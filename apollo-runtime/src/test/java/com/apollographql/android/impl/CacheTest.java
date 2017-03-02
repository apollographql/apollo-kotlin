package com.apollographql.android.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.http.DiskLruCacheStore;
import com.apollographql.android.cache.http.HttpCache;
import com.apollographql.android.cache.http.TimeoutEvictionStrategy;
import com.apollographql.android.impl.type.CustomType;
import com.apollographql.android.impl.util.HttpException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.internal.io.FileSystem;
import okhttp3.internal.io.InMemoryFileSystem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class CacheTest {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

  private ApolloClient apolloClient;
  private okhttp3.Request lastHttRequest;
  private okhttp3.Response lastHttResponse;
  private MockCacheStore cacheStore;
  private OkHttpClient okHttpClient;
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public InMemoryFileSystem inMemoryFileSystem = new InMemoryFileSystem();

  @Before public void setUp() {
    CustomTypeAdapter<Date> dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(String value) {
        try {
          return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public String encode(Date value) {
        return DATE_FORMAT.format(value);
      }
    };

    cacheStore = new MockCacheStore();
    cacheStore.delegate = new DiskLruCacheStore(inMemoryFileSystem, new File("/cache/"), Integer.MAX_VALUE);

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
        .httpCache(cacheStore, new TimeoutEvictionStrategy(2, TimeUnit.SECONDS))
        .withCustomTypeAdapter(CustomType.DATETIME, dateCustomTypeAdapter)
        .build();
  }

  @After public void tearDown() {
    try {
      apolloClient.clearCache();
    } catch (Exception ignore) {
    }
  }

  @Test public void prematureDisconnect() throws Exception {
    MockResponse mockResponse = mockResponse("src/test/graphql/allPlanetsResponse.json");
    Buffer truncatedBody = new Buffer();
    truncatedBody.write(mockResponse.getBody(), 16);
    mockResponse.setBody(truncatedBody);
    server.enqueue(mockResponse);

    try {
      Response<AllPlanets.Data> body = apolloClient.newCall(new AllPlanets()).network().execute();
      assertThat(body.isSuccessful()).isTrue();
      fail("expected IOException");
    } catch (IOException expected) {
    }

    checkNoCachedResponse();
  }

  @Test public void cacheDefault() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  @Test public void cacheSeveralResponses() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");

    enqueueResponse("src/test/graphql/productsWithDate.json");
    assertThat(apolloClient.newCall(new ProductsWithDate()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/productsWithDate.json");

    enqueueResponse("src/test/graphql/productsWithUnsupportedCustomScalarTypes.json");
    assertThat(apolloClient.newCall(new ProductsWithUnsupportedCustomScalarTypes()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/productsWithUnsupportedCustomScalarTypes.json");
  }

  //TODO we need resolution for this as we get GraphQL error (not http) and in some cases there can be response with data and error
  //https://github.com/apollographql/apollo-android/issues/251
//  @Test public void cacheErrorResponse() throws Exception {
//    MockResponse mockResponse = mockResponse("src/test/graphql/errorResponse.json");
//    server.enqueue(mockResponse);
//
//    ApolloCall call = apolloClient.newCall(new AllPlanets());
//    Response<AllPlanets.Data> body = call.execute();
//    assertThat(body.isSuccessful()).isFalse();
//    checkNoCachedResponse(call);
//  }

  @Test public void noCacheStore() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .build();

    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
  }

  @Test public void networkOnly() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).network().execute().isSuccessful()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
    checkNoCachedResponse();
  }

  @Test public void cacheOnlyHit() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.takeRequest()).isNotNull();

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).cache().execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  @Test public void cacheOnlyMiss() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    try {
      apolloClient.newCall(new AllPlanets()).cache().execute();
      Assert.fail("expected to fail with HttpException");
    } catch (HttpException expected) {
    } catch (Exception e) {
      fail("expected to fail with HttpException");
    }
  }

  @Test public void cacheNonStale() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.takeRequest()).isNotNull();

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  @Test public void cacheStale() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  @Test public void cacheStaleBeforeNetwork() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).networkBeforeStale().execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  @Test public void cacheStaleBeforeNetworkError() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    server.enqueue(new MockResponse().setResponseCode(504).setBody(""));
    apolloClient.newCall(new AllPlanets()).networkBeforeStale().execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  @Test public void cacheUpdate() throws Exception {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    enqueueResponse("src/test/graphql/allPlanetsResponse2.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse("src/test/graphql/allPlanetsResponse2.json");
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();

    enqueueResponse("src/test/graphql/allPlanetsResponse2.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("src/test/graphql/allPlanetsResponse2.json");
  }

  @Test public void fileSystemUnavailable() throws IOException {
    cacheStore.delegate = new DiskLruCacheStore(new NoFileSystem(), new File("/cache/"), Integer.MAX_VALUE);
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
  }

  @Test public void fileSystemWriteFailure() throws IOException {
    FaultyCacheStore faultyCacheStore = new FaultyCacheStore(FileSystem.SYSTEM);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_HEADER_WRITE);
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_BODY_WRITE);
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
  }

  @Test public void fileSystemReadFailure() throws IOException {
    FaultyCacheStore faultyCacheStore = new FaultyCacheStore(inMemoryFileSystem);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_HEADER_READ);
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(2);

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_BODY_READ);
    try {
      apolloClient.newCall(new AllPlanets()).execute();
      fail("expected exception");
    } catch (Exception expected) {
    }
    assertThat(server.getRequestCount()).isEqualTo(2);
  }

  @Test public void prefetchDefault() throws IOException {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.prefetch(new AllPlanets()).execute();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");

    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    assertThat(apolloClient.newCall(new AllPlanets()).cache().execute().isSuccessful()).isTrue();
  }

  @Test public void prefetchNoCacheStore() throws IOException {
     ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .build();

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    apolloClient.prefetch(new AllPlanets()).execute();
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
  }

  @Test public void prefetchFileSystemWriteFailure() throws IOException {
    FaultyCacheStore faultyCacheStore = new FaultyCacheStore(FileSystem.SYSTEM);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_HEADER_WRITE);
    try {
      apolloClient.prefetch(new AllPlanets()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_BODY_WRITE);
    try {
      apolloClient.prefetch(new AllPlanets()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();
  }

  @Test public void expireAfterRead() throws IOException {
    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");

    assertThat(apolloClient.newCall(new AllPlanets()).expireAfterRead().execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
    try {
      apolloClient.newCall(new AllPlanets()).cache().execute();
      fail("exception expected");
    } catch (Exception expected) {
    }

    enqueueResponse("src/test/graphql/allPlanetsResponse.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("src/test/graphql/allPlanetsResponse.json");
  }

  private void enqueueResponse(String fileName) throws IOException {
    server.enqueue(mockResponse(fileName));
  }

  private void checkCachedResponse(String fileName) throws IOException {
    String cacheKey = BaseApolloCall.cacheKey(lastHttRequest.body());
    okhttp3.Response response = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(response).isNotNull();
    assertThat(response.body().source().readUtf8()).isEqualTo(Files.toString(new File(fileName), Charsets.UTF_8));
    response.body().source().close();
  }

  private void checkNoCachedResponse() throws IOException {
    String cacheKey = lastHttRequest.header(HttpCache.CACHE_KEY_HEADER);
    okhttp3.Response cachedResponse = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(cachedResponse).isNull();
  }

  private static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Files.toString(new File(fileName), Charsets.UTF_8), 32);
  }
}