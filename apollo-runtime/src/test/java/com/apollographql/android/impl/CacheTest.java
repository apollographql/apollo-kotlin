package com.apollographql.android.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.DiskLruCacheStore;
import com.apollographql.android.cache.HttpCache;
import com.apollographql.android.cache.TimeoutEvictionStrategy;
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
import okhttp3.Request;
import okhttp3.internal.io.InMemoryFileSystem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static com.google.common.truth.Truth.assertThat;

public class CacheTest {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

  private ApolloClient apolloClient;
  private okhttp3.Response lastHttResponse;
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public InMemoryFileSystem fileSystem = new InMemoryFileSystem();

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

    DiskLruCacheStore diskLruCacheStore = new DiskLruCacheStore(fileSystem, new File("/cache/"), Integer.MAX_VALUE);

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .addInterceptor(new Interceptor() {
          @Override public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            okhttp3.Response response = chain.proceed(request);
            lastHttResponse = response;
            return response;
          }
        })
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .httpCache(diskLruCacheStore, new TimeoutEvictionStrategy(2, TimeUnit.SECONDS))
        .withCustomTypeAdapter(CustomType.DATETIME, dateCustomTypeAdapter)
        .build();
  }

  @After public void tearDown() throws Exception {
    apolloClient.clearCache();
  }

  @Test public void prematureDisconnect() throws Exception {
    MockResponse mockResponse = mockResponse("src/test/graphql/allPlanetsResponse.json");
    Buffer truncatedBody = new Buffer();
    truncatedBody.write(mockResponse.getBody(), 16);
    mockResponse.setBody(truncatedBody);
    server.enqueue(mockResponse);

    ApolloCall call = apolloClient.newCall(new AllPlanets()).network();
    try {
      Response<AllPlanets.Data> body = call.execute();
      assertThat(body.isSuccessful()).isTrue();
      Assert.fail("expected IOException");
    } catch (IOException expected) {
    }

    checkNoCachedResponse(call);
  }

  @Test public void cacheDefault() throws Exception {
    MockResponse mockResponse = mockResponse("src/test/graphql/allPlanetsResponse.json");
    server.enqueue(mockResponse);

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    Response<AllPlanets.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));
  }

  @Test public void cacheSeveralResponses() throws Exception {
    MockResponse mockResponse = mockResponse("src/test/graphql/allPlanetsResponse.json");
    server.enqueue(mockResponse);

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    Response<AllPlanets.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));

    mockResponse = mockResponse("src/test/graphql/productsWithDate.json");
    server.enqueue(mockResponse);

    call = apolloClient.newCall(new ProductsWithDate());
    body = call.execute();
    assertThat(body.isSuccessful()).isTrue();
    checkCachedResponse(call, new File("src/test/graphql/productsWithDate.json"));

    mockResponse = mockResponse("src/test/graphql/productsWithUnsupportedCustomScalarTypes.json");
    server.enqueue(mockResponse);

    call = apolloClient.newCall(new ProductsWithUnsupportedCustomScalarTypes());
    body = call.execute();
    assertThat(body.isSuccessful()).isTrue();
    checkCachedResponse(call, new File("src/test/graphql/productsWithUnsupportedCustomScalarTypes.json"));
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
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder().build())
        .build();

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    Response<AllPlanets.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    checkNoCachedResponse(call);
  }

  @Test public void networkOnly() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    ApolloCall call = apolloClient.newCall(new AllPlanets()).network();
    Response<AllPlanets.Data> body = call.execute();
    assertThat(server.takeRequest()).isNotNull();
    assertThat(body.isSuccessful()).isTrue();

    checkNoCachedResponse(call);

    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNull();
  }

  @Test public void cacheOnlyHit() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.takeRequest()).isNotNull();

    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    ApolloCall call = apolloClient.newCall(new AllPlanets()).cache();
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
  }

  @Test public void cacheOnlyMiss() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));
    ApolloCall call = apolloClient.newCall(new AllPlanets()).cache();
    try {
      Response<AllPlanets.Data> body = call.execute();
      Assert.fail("expected to fail with HttpException");
    } catch (HttpException expected) {
    } catch (Exception e) {
      Assert.fail("expected to fail with HttpException");
    }
  }

  @Test public void cacheNonStale() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.takeRequest()).isNotNull();

    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
  }

  @Test public void cacheStale() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
  }

  @Test public void cacheStaleBeforeNetwork() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    ApolloCall call = apolloClient.newCall(new AllPlanets()).networkBeforeStale();
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
  }

  @Test public void cacheStaleBeforeNetworkError() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));
    apolloClient.newCall(new AllPlanets()).execute();
    assertThat(server.getRequestCount()).isEqualTo(1);

    server.enqueue(new MockResponse().setResponseCode(504).setBody(""));

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    ApolloCall call = apolloClient.newCall(new AllPlanets()).networkBeforeStale();
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
  }

  @Test public void cacheUpdate() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(1);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse.json"));

    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse2.json"));

    Thread.sleep(TimeUnit.SECONDS.toMillis(3));

    call = apolloClient.newCall(new AllPlanets());
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse2.json"));
    assertThat(lastHttResponse.networkResponse()).isNotNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();

    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse2.json"));

    call = apolloClient.newCall(new AllPlanets());
    call.execute();
    assertThat(server.getRequestCount()).isEqualTo(2);
    checkCachedResponse(call, new File("src/test/graphql/allPlanetsResponse2.json"));
    assertThat(lastHttResponse.networkResponse()).isNull();
    assertThat(lastHttResponse.cacheResponse()).isNotNull();
  }

  private void checkCachedResponse(ApolloCall call, File content) throws IOException {
    Request request = ((RealApolloCall) call).httpCall.request();
    String cacheKey = RealApolloCall.cacheKey(request.body());
    okhttp3.Response response = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(response).isNotNull();
    assertThat(response.body().source().readUtf8()).isEqualTo(Files.toString(content, Charsets.UTF_8));
    response.body().source().close();
  }

  private void checkNoCachedResponse(ApolloCall call) throws IOException {
    Request request = ((RealApolloCall) call).httpCall.request();
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    okhttp3.Response cachedResponse = apolloClient.cachedHttpResponse(cacheKey);
    assertThat(cachedResponse).isNull();
  }

  private static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Files.toString(new File(fileName), Charsets.UTF_8), 32);
  }
}
