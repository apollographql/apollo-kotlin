package com.apollographql.android.impl;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.http.DiskLruCacheStore;
import com.apollographql.android.cache.http.HttpCache;
import com.apollographql.android.cache.http.HttpCacheControl;
import com.apollographql.android.cache.http.TimeoutEvictionStrategy;
import com.apollographql.android.impl.httpcache.AllFilms;
import com.apollographql.android.impl.httpcache.AllPlanets;
import com.apollographql.android.impl.httpcache.DroidDetails;
import com.apollographql.android.impl.httpcache.type.CustomType;
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
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

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
        .withCustomTypeAdapter(CustomType.DATE, dateCustomTypeAdapter)
        .build();
  }

  @After public void tearDown() {
    try {
      apolloClient.clearCache();
    } catch (Exception ignore) {
    }
  }

  @Test public void prematureDisconnect() throws Exception {
    MockResponse mockResponse = mockResponse("/HttpCacheTestAllPlanets.json");
    Buffer truncatedBody = new Buffer();
    truncatedBody.write(mockResponse.getBody(), 16);
    mockResponse.setBody(truncatedBody);
    server.enqueue(mockResponse);

    try {
      Response<AllPlanets.Data> body = apolloClient.newCall(new AllPlanets())
          .httpCacheControl(HttpCacheControl.NETWORK_ONLY).execute();
      assertThat(body.isSuccessful()).isTrue();
      fail("expected IOException");
    } catch (IOException expected) {
    }

    checkNoCachedResponse();
  }

  @Test public void cacheDefault() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheSeveralResponses() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    enqueueResponse("/HttpCacheTestDroidDetails.json");
    assertThat(apolloClient.newCall(new DroidDetails()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestDroidDetails.json");

    enqueueResponse("/HttpCacheTestAllFilms.json");
    assertThat(apolloClient.newCall(new AllFilms()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllFilms.json");
  }

  //TODO we need resolution for this as we get GraphQL error (not http) and in some cases there can be response with data and error
  //https://github.com/apollographql/apollo-android/issues/251
//  @Test public void cacheErrorResponse() throws Exception {
//    MockResponse mockResponse = mockResponse("/errorResponse.json");
//    server.enqueue(mockResponse);
//
//    ApolloCall call = apolloClient.newCall(new AllPlanets());
//    Response<AllPlanets.Data> body = call.execute();
//    assertThat(body.isSuccessful()).isFalse();
//    checkNoCachedResponse(call);
//  }

  @Test public void noCacheStore() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .build();

    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
  }

  @Test public void networkOnly() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.NETWORK_ONLY).execute().isSuccessful()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
    checkNoCachedResponse();
  }

  @Test public void cacheOnlyHit() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.takeRequest()).isNotNull();

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.CACHE_ONLY).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheOnlyMiss() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    try {
      apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.CACHE_ONLY).execute();
      Assert.fail("expected to fail with HttpException");
    } catch (HttpException expected) {
    } catch (Exception e) {
      fail("expected to fail with HttpException");
    }
  }

  @Test public void cacheNonStale() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.takeRequest()).isNotNull();

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheStale() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheStaleBeforeNetwork() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.NETWORK_BEFORE_STALE).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheStaleBeforeNetworkError() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    server.enqueue(new MockResponse().setResponseCode(504).setBody(""));
    apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.NETWORK_BEFORE_STALE).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheUpdate() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    enqueueResponse("/HttpCacheTestAllPlanets2.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse("/HttpCacheTestAllPlanets2.json");
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();

    enqueueResponse("/HttpCacheTestAllPlanets2.json");
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
    checkCachedResponse("/HttpCacheTestAllPlanets2.json");
  }

  @Test public void fileSystemUnavailable() throws IOException {
    cacheStore.delegate = new DiskLruCacheStore(new NoFileSystem(), new File("/cache/"), Integer.MAX_VALUE);
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
  }

  @Test public void fileSystemWriteFailure() throws IOException {
    FaultyCacheStore faultyCacheStore = new FaultyCacheStore(FileSystem.SYSTEM);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_HEADER_WRITE);
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_BODY_WRITE);
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkNoCachedResponse();
  }

  @Test public void fileSystemReadFailure() throws IOException {
    FaultyCacheStore faultyCacheStore = new FaultyCacheStore(inMemoryFileSystem);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_HEADER_READ);
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(2);

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_BODY_READ);
    try {
      apolloClient.newCall(new AllPlanets()).execute();
      fail("expected exception");
    } catch (Exception expected) {
    }
    assertThat(server.getRequestCount()).isEqualTo(2);
  }

  @Test public void prefetchDefault() throws IOException {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.prefetch(new AllPlanets()).execute();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    assertThat(apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.CACHE_ONLY).execute().isSuccessful()).isTrue();
  }

  @Test public void prefetchNoCacheStore() throws IOException {
     ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .build();

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    apolloClient.prefetch(new AllPlanets()).execute();
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
  }

  @Test public void prefetchFileSystemWriteFailure() throws IOException {
    FaultyCacheStore faultyCacheStore = new FaultyCacheStore(FileSystem.SYSTEM);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_HEADER_WRITE);
    try {
      apolloClient.prefetch(new AllPlanets()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyCacheStore.FailStrategy.FAIL_BODY_WRITE);
    try {
      apolloClient.prefetch(new AllPlanets()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();
  }

  @Test public void expireAfterRead() throws IOException {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    assertThat(apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.EXPIRE_AFTER_READ).execute()
        .isSuccessful()).isTrue();
    checkNoCachedResponse();
    try {
      apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.CACHE_ONLY).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  @Test public void cacheNetworkError() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(504).setBody(""));
    try {
      apolloClient.newCall(new AllPlanets()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    assertThat(apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.CACHE_ONLY).execute()
        .isSuccessful()).isTrue();
  }

  @Test public void networkFirst() throws Exception {
    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).execute().isSuccessful()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(1);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");

    enqueueResponse("/HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.newCall(new AllPlanets()).httpCacheControl(HttpCacheControl.NETWORK_FIRST).execute()
        .isSuccessful()).isTrue();
    assertThat(server.getRequestCount()).isEqualTo(2);
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
    checkCachedResponse("/HttpCacheTestAllPlanets.json");
  }

  private void enqueueResponse(String fileName) throws IOException {
    server.enqueue(mockResponse(fileName));
  }

  private void checkCachedResponse(String fileName) throws IOException {
    String cacheKey = ApolloServerInterceptor.cacheKey(lastHttRequest.body());
    okhttp3.Response response = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(response).isNotNull();
    assertThat(response.body().source().readUtf8()).isEqualTo(Utils.readFileToString(getClass(),fileName));
    response.body().source().close();
  }

  private void checkNoCachedResponse() throws IOException {
    String cacheKey = lastHttRequest.header(HttpCache.CACHE_KEY_HEADER);
    okhttp3.Response cachedResponse = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(cachedResponse).isNull();
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), fileName), 32);
  }
}
