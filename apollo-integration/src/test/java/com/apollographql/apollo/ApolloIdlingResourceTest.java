package com.apollographql.apollo;


import com.google.common.truth.Truth;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.espresso.ApolloIdlingResource;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
  public void onCallBeingQueued_IdlingResourceNotIdle() throws IOException, TimeoutException, InterruptedException {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID));

    final NamedCountDownLatch latch = new NamedCountDownLatch("latch", 1);
    EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .serverUrl(server.url("/"))
        .build();
    idlingResource = ApolloIdlingResource.create("testIdlingResource", apolloClient);

    assertThat(idlingResource.isIdleNow()).isTrue();

    apolloClient.newCall(query).enqueue(new ApolloCall.Callback<EpisodeHeroName.Data>() {
      @Override public void onResponse(@Nonnull Response<EpisodeHeroName.Data> response) {
        latch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    assertThat(idlingResource.isIdleNow()).isFalse();

    latch.await(TIME_OUT_SECONDS, TimeUnit.SECONDS);

    Thread.sleep(2000);

    assertThat(idlingResource.isIdleNow()).isTrue();
  }


  private MockResponse mockResponse(String fileName) throws IOException {
    return new MockResponse().setChunkedBody(Utils.readFileToString(getClass(), "/" + fileName), 32);
  }
}