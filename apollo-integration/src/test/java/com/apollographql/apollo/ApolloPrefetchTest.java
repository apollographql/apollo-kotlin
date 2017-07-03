package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore;
import com.apollographql.apollo.cache.http.HttpCache;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.internal.io.FileSystem;
import okhttp3.internal.io.InMemoryFileSystem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class ApolloPrefetchTest {
  private static final long TIME_OUT_SECONDS = 3;
  private ApolloClient apolloClient;
  private MockWebServer server;
  @Rule public InMemoryFileSystem inMemoryFileSystem = new InMemoryFileSystem();
  private okhttp3.Request lastHttRequest;
  private okhttp3.Response lastHttResponse;
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
        .httpCacheStore(cacheStore)
        .build();
  }

  @After public void tearDown() {
    try {
      apolloClient.clearHttpCache();
      server.shutdown();
    } catch (Exception ignore) {
    }
  }

  @Test
  public void cancelPrefetchBeforeEnqueueTriggersOnFailure() throws Exception {
    final NamedCountDownLatch responseLatch
        = new NamedCountDownLatch("cancelPrefetchBeforeEnqueueTriggersOnFailure", 1);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    final AtomicReference<ApolloException> errorRef = new AtomicReference<>();
    ApolloCall<EpisodeHeroNameQuery.Data> apolloCall = apolloClient.query(query);

    apolloCall.cancel();

    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        responseLatch.countDown();
        errorRef.set(e);
      }
    });
    responseLatch.awaitOrThrowWithTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(errorRef.get()).isInstanceOf(ApolloCanceledException.class);
  }

  @Test
  public void cancelAfterEnqueuingHasNoCallback() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("cancelAfterEnqueuingHasNoCallback", 1);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
        .setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    ApolloPrefetch apolloPrefetch = apolloClient.prefetch(query);
    final AtomicReference<String> errorState = new AtomicReference<>(null);
    apolloPrefetch.enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
        errorState.set("onSuccess should not be called after cancel");
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        errorState.set("onFailure should not be called after cancel");
        responseLatch.countDown();
      }
    });

    Thread.sleep(500);
    apolloPrefetch.cancel();
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(errorState.get()).isNull();
  }

  @Test
  public void apolloCanceledExceptionExecute() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("apolloCanceledExceptionExecute", 1);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    server.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
        .setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    final AtomicReference<ApolloException> errorRef = new AtomicReference<>();
    final ApolloPrefetch apolloCall = apolloClient.prefetch(query);
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          apolloCall.execute();
        } catch (ApolloException e) {
          errorRef.set(e);
        }
        responseLatch.countDown();
      }
    }).start();

    Thread.sleep(500);
    apolloCall.cancel();
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.HOURS);

    assertThat(errorRef.get()).isInstanceOf(ApolloCanceledException.class);
  }

  @Test public void prefetchDefault() throws IOException, ApolloException {
    enqueueResponse("HttpCacheTestAllPlanets.json");
    apolloClient.prefetch(new AllPlanetsQuery()).execute();
    checkCachedResponse("HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.query(new AllPlanetsQuery())
        .httpCachePolicy(HttpCachePolicy.CACHE_ONLY.expireAfter(2, TimeUnit.SECONDS)).execute()
        .hasErrors()).isFalse();
  }

  @Test public void prefetchNoCacheStore() throws IOException, ApolloException {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .build();

    enqueueResponse("HttpCacheTestAllPlanets.json");
    apolloClient.prefetch(new AllPlanetsQuery()).execute();
    enqueueResponse("HttpCacheTestAllPlanets.json");
    assertThat(apolloClient.query(new AllPlanetsQuery()).execute().hasErrors()).isFalse();
  }

  @Test public void prefetchFileSystemWriteFailure() throws IOException {
    FaultyHttpCacheStore faultyCacheStore = new FaultyHttpCacheStore(FileSystem.SYSTEM);
    cacheStore.delegate = faultyCacheStore;

    enqueueResponse("HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_HEADER_WRITE);
    try {
      apolloClient.prefetch(new AllPlanetsQuery()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();

    enqueueResponse("HttpCacheTestAllPlanets.json");
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_BODY_WRITE);
    try {
      apolloClient.prefetch(new AllPlanetsQuery()).execute();
      fail("exception expected");
    } catch (Exception expected) {
    }
    checkNoCachedResponse();
  }

  private void enqueueResponse(String fileName) throws IOException {
    server.enqueue(mockResponse(fileName));
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

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
