package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.RealApolloCall;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloCallTrackerTest {

  private static final String SERVER_URL = "http://localhost:1234";
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
  public void testAsyncPrefetchInProgress() throws InterruptedException {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build();
    server.enqueue(new MockResponse());

    ApolloPrefetch prefetch = apolloClient.prefetch(new Operation<Operation.Data, Object, Operation.Variables>() {
      @Override public String queryDocument() {
        return null;
      }

      @Override public Variables variables() {
        return null;
      }

      @Override public ResponseFieldMapper<Data> responseFieldMapper() {
        return null;
      }

      @Override public Object wrapData(Data data) {
        return null;
      }
    });

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(0);

    prefetch.enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(1);
  }

  @Test
  public void testAsyncCallInProgress() {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build();
    server.enqueue(new MockResponse());

    RealApolloCall<Object> apolloCall = (RealApolloCall<Object>) apolloClient.newCall(new Operation<Operation.Data, Object, Operation.Variables>() {
      @Override public String queryDocument() {
        return null;
      }

      @Override public Variables variables() {
        return null;
      }

      @Override public ResponseFieldMapper<Data> responseFieldMapper() {
        return new ResponseFieldMapper<Data>() {
          @Override public Data map(ResponseReader responseReader) throws IOException {
            return new Data() {
            };
          }
        };
      }

      @Override public Object wrapData(Data data) {
        return null;
      }
    });

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(0);

    apolloCall.enqueue(new ApolloCall.Callback<Object>() {
      @Override public void onResponse(@Nonnull Response<Object> response) {

      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    assertThat(apolloClient.getRunningCallsCount()).isEqualTo(1);
  }
}
