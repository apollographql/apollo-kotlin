package com.apollographql.apollo;


import com.apollographql.apollo.espresso.ApolloIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloIdlingResourceTest {

  private ApolloIdlingResource idlingResource;
  private ApolloClient apolloClient;
  private MockWebServer server;

  private static final String IDLING_RESOURCE_NAME = "apolloIdlingResource";
  private OkHttpClient okHttpClient;

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
    }
  }

  @Test
  public void onNullApolloClientPassed_NullPointerExceptionIsThrown() {
    try {
      idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, null);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NullPointerException.class);
    }
  }

  @Test
  public void checkValidIdlingResourceNameIsRegistered() {

    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .serverUrl(server.url("/"))
        .build();

    idlingResource = ApolloIdlingResource.create(IDLING_RESOURCE_NAME, apolloClient);

    assertThat(idlingResource.getName()).isEqualTo(IDLING_RESOURCE_NAME);
  }



}