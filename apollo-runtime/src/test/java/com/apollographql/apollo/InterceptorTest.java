package com.apollographql.apollo;

import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.HttpException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

public class InterceptorTest {
  private ApolloClient apolloClient;
  @Rule public final MockWebServer server = new MockWebServer();

  @Before public void setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder().build())
        .build();
  }

  @Test public void httpException() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));

    try {
      apolloClient.newCall(new Query() {
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
      }).execute();
      fail("Expected HttpException");
    } catch (HttpException httpException) {
      assertThat(httpException.code()).isEqualTo(401);
      assertThat(httpException.message()).isEqualTo("Client Error");
      assertThat(httpException.rawResponse().body().string()).isEqualTo("Unauthorized request!");
      assertThat(httpException.getMessage()).isEqualTo("HTTP 401 Client Error");
    }
  }
}
