package com.apollographql.apollo;

import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.json.JsonEncodingException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.fail;

@SuppressWarnings("unchecked") public class ApolloExceptionTest {
  @Rule public final MockWebServer server = new MockWebServer();
  private ApolloClient apolloClient;
  private Query emptyQuery;

  @Before public void setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build())
        .build();

    emptyQuery = new Query() {
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
  }

  @Test public void httpException() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));
    try {
      apolloClient.newCall(emptyQuery).execute();
      fail("Expected ApolloHttpException");
    } catch (ApolloHttpException e) {
      assertThat(e.code()).isEqualTo(401);
      assertThat(e.message()).isEqualTo("Client Error");
      assertThat(e.rawResponse().body().string()).isEqualTo("Unauthorized request!");
      assertThat(e.getMessage()).isEqualTo("HTTP 401 Client Error");
    }
  }

  @Test public void httpExceptionAsync() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));
    apolloClient.newCall(emptyQuery).enqueue(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        fail("Expected ApolloHttpException");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected ApolloHttpException");
      }

      @Override public void onHttpError(@Nonnull ApolloHttpException e) {

      }
    });
  }

  @Test public void httpExceptionPrefetch() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));
    apolloClient.prefetch(emptyQuery).enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
        fail("Expected ApolloHttpException");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected ApolloHttpException");
      }

      @Override public void onHttpError(@Nonnull ApolloHttpException e) {
      }
    });
  }

  @Test public void testTimeoutException() throws Exception {
    try {
      apolloClient.newCall(emptyQuery).execute();
      fail("Expected ApolloNetworkException");
    } catch (ApolloNetworkException e) {
      assertThat(e.getMessage()).isEqualTo("Failed to execute http call");
      assertThat(e.getCause().getClass()).isEqualTo(SocketTimeoutException.class);
    }
  }

  @Test public void testTimeoutExceptionAsync() throws Exception {
    apolloClient.newCall(emptyQuery).enqueue(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        fail("Expected ApolloNetworkException");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected ApolloNetworkException");
      }

      @Override public void onNetworkError(@Nonnull ApolloNetworkException e) {
      }
    });
  }

  @Test public void testTimeoutExceptionPrefetch() throws Exception {
    apolloClient.prefetch(emptyQuery).enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
        fail("Expected ApolloNetworkException");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected ApolloNetworkException");
      }

      @Override public void onNetworkError(@Nonnull ApolloNetworkException e) {
      }
    });
  }

  @Test public void testParseException() throws Exception {
    server.enqueue(new MockResponse().setBody("Noise"));
    try {
      apolloClient.newCall(emptyQuery).execute();
      fail("Expected ApolloParseException");
    } catch (ApolloParseException e) {
      assertThat(e.getMessage()).isEqualTo("Failed to parse http response");
      assertThat(e.getCause().getClass()).isEqualTo(JsonEncodingException.class);
    }
  }

  @Test public void testParseExceptionAsync() throws Exception {
    server.enqueue(new MockResponse().setBody("Noise"));
    apolloClient.newCall(emptyQuery).enqueue(new ApolloCall.Callback() {
      @Override public void onResponse(@Nonnull Response response) {
        fail("Expected ApolloParseException");
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        fail("Expected ApolloParseException");
      }

      @Override public void onParseError(@Nonnull ApolloParseException e) {
      }
    });
  }
}
