package com.apollographql.apollo;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
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

public class ApolloPrefetchTest {

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
  public void testApolloPrefetchNotCalled_WhenCanceled() throws IOException, InterruptedException {

    final NamedCountDownLatch responseLatch = new NamedCountDownLatch("responseLatch", 1);

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

  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}
