package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

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
  public void testApolloCallNotCalled_WhenCanceled() throws IOException, InterruptedException {

    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();
    mockWebServer.enqueue(mockResponse("EpisodeHeroNameResponseWithId.json"));

    ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.newCall(query);

    apolloCall.cancel();

    apolloCall.enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        responseLatch.countDown();
        if (responseLatch.getCount() == 0) {
          Assert.fail("Received callback, although apollo call has already been canceled");
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        responseLatch.countDown();
        Assert.fail(e.getMessage());
      }
    });

    //Wait for 3 seconds to check that callback is not called.
    //Test is successful if timeout is reached.
    responseLatch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);
  }

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
