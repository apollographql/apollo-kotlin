package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;

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
import static org.junit.Assert.fail;

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
  public void apolloCallNotCalled_WhenCanceled() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("apolloCallNotCalled_WhenCanceled", 1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.query(query);

    apolloCall.cancel();

    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        responseLatch.countDown();
        if (responseLatch.getCount() == 0) {
          fail("Received callback, although apollo call has already been canceled");
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        responseLatch.countDown();
        fail(e.getMessage());
      }
    });

    //Wait for 3 seconds to check that callback is not called.
    //Test is successful if timeout is reached.
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  @Test
  public void apolloCanceledExceptionEnqueue() throws Exception {
    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("apolloCanceledExceptionEnqueue", 1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json")
        .setBodyDelay(TIME_OUT_SECONDS, TimeUnit.SECONDS));

    final AtomicReference<ApolloException> errorRef = new AtomicReference<>();
    ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.query(query);
    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
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
    final ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.query(query);
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
