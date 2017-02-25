package com.apollographql.android.impl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.DiskLruCacheStore;
import com.apollographql.android.cache.HttpCache;
import com.apollographql.android.cache.HttpCacheInterceptor;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.internal.io.InMemoryFileSystem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static com.google.common.truth.Truth.assertThat;

public class CacheTest {
  private ApolloClient<ApolloCall> apolloClient;
  private HttpCache httpCache;
  @Rule public final MockWebServer server = new MockWebServer();
  @Rule public InMemoryFileSystem fileSystem = new InMemoryFileSystem();

  @Before public void setUp() {
    DiskLruCacheStore diskLruCacheStore = new DiskLruCacheStore(fileSystem, new File("/cache/"), Integer.MAX_VALUE);
    httpCache = new HttpCache(diskLruCacheStore);

    apolloClient = ApolloClient.<ApolloCall>builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder().build())
        .httpCache(httpCache)
        .withCallAdapter(new ApolloCallAdapter())
        .build();
  }

  @After public void tearDown() throws Exception {
    httpCache.delete();
  }

  @Test public void prematureDisconnect() throws Exception {
    MockResponse mockResponse = mockResponse("src/test/graphql/allPlanetsResponse.json");
    Buffer truncatedBody = new Buffer();
    truncatedBody.write(mockResponse.getBody(), 16);
    mockResponse.setBody(truncatedBody);

    server.enqueue(mockResponse);

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    try {
      Response<AllPlanets.Data> body = call.execute();
      assertThat(body.isSuccessful()).isTrue();
      Assert.fail("expected IOException");
    } catch (IOException expected) {
    }

    String cacheKey = ((RealApolloCall) call).httpCall.request().header(HttpCacheInterceptor.CACHE_KEY_HEADER);
    okhttp3.Response cachedResponse = httpCache.read(cacheKey);
    assertThat(cachedResponse).isNull();
  }

  @Test public void cacheSuccess() throws Exception {
    MockResponse mockResponse = mockResponse("src/test/graphql/allPlanetsResponse.json");
    server.enqueue(mockResponse);

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    Response<AllPlanets.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    String cacheKey = ((RealApolloCall) call).httpCall.request().header(HttpCacheInterceptor.CACHE_KEY_HEADER);
    okhttp3.Response cachedResponse = httpCache.read(cacheKey);
    assertThat(cachedResponse).isNotNull();

    assertThat(cachedResponse.body().source().readString(Charsets.UTF_8))
        .isEqualTo(Files.toString(new File("src/test/graphql/allPlanetsResponse.json"), Charsets.UTF_8));

    cachedResponse.body().source().close();
  }

  @Test public void noCache() throws Exception {
    server.enqueue(mockResponse("src/test/graphql/allPlanetsResponse.json"));

    ApolloClient<ApolloCall> apolloClient = ApolloClient.<ApolloCall>builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder().build())
        .withCallAdapter(new ApolloCallAdapter())
        .build();

    ApolloCall call = apolloClient.newCall(new AllPlanets());
    Response<AllPlanets.Data> body = call.execute();
    assertThat(body.isSuccessful()).isTrue();

    Request request = ((RealApolloCall) call).httpCall.request();
    String cacheKey = RealApolloCall.cacheKey(request.body());
    okhttp3.Response cachedResponse = httpCache.read(cacheKey);
    assertThat(cachedResponse).isNull();
  }

  private static MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Files.toString(new File(fileName), Charsets.UTF_8), 32);
  }
}
