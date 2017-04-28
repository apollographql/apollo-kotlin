package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.RealApolloCall;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloCallTrackerTest {

  private static final String SERVER_URL = "http://localhost:1234";
  private static final int TIMEOUT_SECONDS = 2;
  private static final Query EMPTY_QUERY = new Query() {
    @Override public String queryDocument() {
      return "";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) throws IOException {
          return null;
        }
      };
    }

    @Override public Object wrapData(Data data) {
      return data;
    }
  };

  private OkHttpClient okHttpClient;
  private MockWebServer server;

  @Before
  public void setUp() throws Exception {
    okHttpClient = new OkHttpClient
        .Builder()
        .build();
    server = new MockWebServer();
  }

  @Test
  public void testRunningCallsCount_whenAsyncPrefetchCallIsMade() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build();
    server.enqueue(createMockResponse());

    ApolloPrefetch prefetch = apolloClient.prefetch(EMPTY_QUERY);

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(0);

    prefetch.enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
        latch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(1);
    latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(0);
  }

  @Test
  public void testRunningCallsCount_whenAsyncApolloCallIsMade() throws InterruptedException {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build();
    server.enqueue(createMockResponse());

    RealApolloCall<Object> apolloCall = (RealApolloCall<Object>) apolloClient.newCall(EMPTY_QUERY);

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(0);

    apolloCall.enqueue(new ApolloCall.Callback<Object>() {
      @Override public void onResponse(@Nonnull Response<Object> response) {
        countDownLatch.countDown();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(1);
    countDownLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(0);
  }



  private MockResponse createMockResponse() {

    return new MockResponse().setResponseCode(200).setBody(new StringBuilder()
        .append("{")
        .append("  \"errors\": [")
        .append("    {")
        .append("      \"message\": \"Cannot query field \\\"names\\\" on type \\\"Species\\\".\",")
        .append("      \"locations\": [")
        .append("        {")
        .append("          \"line\": 3,")
        .append("          \"column\": 5")
        .append("        }")
        .append("      ]")
        .append("    }")
        .append("  ]")
        .append("}")
        .toString());
  }
}
