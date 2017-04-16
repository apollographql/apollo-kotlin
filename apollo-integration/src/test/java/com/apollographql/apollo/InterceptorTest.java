package com.apollographql.apollo;


import com.apollographql.apollo.exception.ApolloException;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

public class InterceptorTest {

  private ApolloClient apolloClient;
  private MockWebServer mockWebServer;

  @Before
  public void setup() {
    mockWebServer = new MockWebServer();
    OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
  }

  @Test
  public void applicationInterceptorCanShortCircuitResponses() throws IOException, ApolloException {

  }

}
