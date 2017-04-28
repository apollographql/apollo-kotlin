package com.apollographql.apollo;


import com.google.common.truth.Truth;

import android.support.test.espresso.IdlingResource;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.espresso.ApolloIdlingResource;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloIdlingResourceTest {

  private ApolloIdlingResource idlingResource;
  private ApolloClient apolloClient;
  private MockWebServer server;
  private OkHttpClient okHttpClient;

  private static final String FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json";
  private static final String IDLING_RESOURCE_NAME = "apolloIdlingResource";

  private static final long TIME_OUT_SECONDS = 3;

  @Before
  public void setup() {
    server = new MockWebServer();
    okHttpClient = new OkHttpClient.Builder()
        .build();
  }

  @After
  public void tearDown() {
    idlingResource = null;
    try {
      server.shutdown();
    } catch (IOException ignored) {

    }
  }

  @Test
  public void onNullNamePassed_NullPointerExceptionIsThrown() {

    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .serverUrl(server.url("/"))
        .build();

    try {
      idlingResource = ApolloIdlingResource.create(null, apolloClient);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NullPointerException.class);
      assertThat(e.getMessage()).isEqualTo("name == null");
    }
  }

  @Test
  public void onNullApolloClientPassed_NullPointerExceptionIsThrown() {
    try {
      idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, null);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NullPointerException.class);
      assertThat(e.getMessage()).isEqualTo("apolloClient == null");
    }
  }

  @Test
  public void checkValidIdlingResourceNameIsRegistered() {

    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .serverUrl(server.url("/"))
        .build();

    idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient);

    Truth.assertThat(idlingResource.getName()).isEqualTo(IDLING_RESOURCE_NAME);
  }

  @Test
  public void checkIsIdleNow_whenCallIsQueued() throws IOException, TimeoutException, InterruptedException {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final NamedCountDownLatch firstLatch = new NamedCountDownLatch("firstLatch", 1);
    final NamedCountDownLatch secondLatch = new NamedCountDownLatch("secondLatch", 1);

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .dispatcher(executorService)
        .serverUrl(server.url("/"))
        .build();

    idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient);
    assertThat(idlingResource.isIdleNow()).isTrue();

    apolloClient.newCall(query).enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        firstLatch.countDown();
        try {
          secondLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        throw new AssertionError("This callback can't be called.");
      }
    });
    assertThat(idlingResource.isIdleNow()).isFalse();

    firstLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    secondLatch.countDown();

    executorService.shutdown();
    executorService.awaitTermination(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(idlingResource.isIdleNow()).isTrue();
  }

  @Test
  public void checkIdlingResourceTransition_whenCallIsQueued() throws IOException, ApolloException {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .dispatcher(Utils.immediateExecutorService())
        .serverUrl(server.url("/"))
        .build();

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    final AtomicInteger counter = new AtomicInteger(1);
    idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient);

    idlingResource.registerIdleTransitionCallback(new IdlingResource.ResourceCallback() {
      @Override public void onTransitionToIdle() {
        counter.decrementAndGet();
      }
    });

    assertThat(counter.get()).isEqualTo(1);
    apolloClient.newCall(query).execute();
    assertThat(counter.get()).isEqualTo(0);
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}