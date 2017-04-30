package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.http.DiskLruCacheStore;
import com.apollographql.apollo.cache.http.TimeoutEvictionStrategy;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.internal.io.InMemoryFileSystem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloPrefetchTest {
  private static final long TIME_OUT_SECONDS = 3;
  private ApolloClient apolloClient;
  private MockWebServer mockWebServer;
  @Rule public InMemoryFileSystem inMemoryFileSystem = new InMemoryFileSystem();
  private MockCacheStore cacheStore;

  @Before
  public void setup() {
    mockWebServer = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    cacheStore = new MockCacheStore();
    cacheStore.delegate = new DiskLruCacheStore(inMemoryFileSystem, new File("/cache/"), Integer.MAX_VALUE);

    apolloClient = ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .httpCache(cacheStore, new TimeoutEvictionStrategy(TIME_OUT_SECONDS, TimeUnit.SECONDS))
        .build();
  }

  @After public void tearDown() {
    try {
      apolloClient.clearHttpCache();
      mockWebServer.shutdown();
    } catch (Exception ignore) {
    }
  }

  @Test
  public void ApolloPrefetchNotCalled_WhenCanceled() throws IOException, InterruptedException {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("ApolloPrefetchNotCalled_WhenCanceled", 1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloPrefetch prefetch = apolloClient.prefetch(query);

    prefetch.cancel();

    prefetch.enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
        responseLatch.countDown();
        if (responseLatch.getCount() == 0) {
          Assert.fail("Received callback, although apollo prefetch has already been canceled");
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        responseLatch.countDown();
        Assert.fail(e.getMessage());
      }
    });

    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void apolloCanceledExceptionEnqueue() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("apolloCanceledExceptionEnqueue", 1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
        .setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    final AtomicReference<ApolloException> errorRef = new AtomicReference<>();
    ApolloPrefetch apolloCall = apolloClient.prefetch(query);
    apolloCall.enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        errorRef.set(e);
        responseLatch.countDown();
      }
    });

    Thread.sleep(500);
    apolloCall.cancel();
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    assertThat(errorRef.get()).isInstanceOf(ApolloCanceledException.class);
  }

  @Test
  public void apolloCanceledExceptionExecute() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("apolloCanceledExceptionExecute", 1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
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

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
