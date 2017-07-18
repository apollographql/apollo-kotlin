package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloCallTest {
  private static final long TIME_OUT_SECONDS = 3;
  private ApolloClient apolloClient;
  private MockWebServer mockWebServer;

  @Before
  public void setup() {
    mockWebServer = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

    apolloClient = ApolloClient.builder()
        .serverUrl(mockWebServer.url("/"))
        .okHttpClient(okHttpClient)
        .build();
  }

  @Test
  public void cancelCallBeforeEnqueueTriggersOnFailure() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("cancelCallBeforeEnqueueTriggersOnFailure", 1);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

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
  public void cancelAfterCallingEnqueueHasNoCallback() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("cancelAfterCallingEnqueueHasNoCallback", 1);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
        .setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    ApolloCall<EpisodeHeroNameQuery.Data> apolloCall = apolloClient.query(query);
    final AtomicReference<String> errorState = new AtomicReference<>(null);
    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroNameQuery.Data> response) {
        errorState.set("onResponse should not be called after cancel");
        responseLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        errorState.set("onFailure should not be called after cancel");
        responseLatch.countDown();
      }
    });

    Thread.sleep(500);
    apolloCall.cancel();
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
    assertThat(errorState.get()).isNull();
  }

  @Test
  public void apolloCanceledExceptionExecute() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("apolloCanceledExceptionExecute", 1);

    EpisodeHeroNameQuery query = EpisodeHeroNameQuery.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
        .setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    final AtomicReference<ApolloException> errorRef = new AtomicReference<>();
    final ApolloCall<EpisodeHeroNameQuery.Data> apolloCall = apolloClient.query(query);
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
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    assertThat(errorRef.get()).isInstanceOf(ApolloCanceledException.class);
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
