package com.apollographql.apollo;

import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.json.JsonEncodingException;
import com.apollographql.apollo.rx2.Rx2Apollo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("unchecked") public class ApolloExceptionTest {
  private static long TIMEOUT_SECONDS = 2;

  @Rule public final MockWebServer server = new MockWebServer();
  private ApolloClient apolloClient;
  private Query emptyQuery;

  @Before public void setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build())
        .build();

    emptyQuery = new Query() {
      OperationName operationName = new OperationName() {
        @Override public String name() {
          return "emptyQuery";
        }
      };

      @Override public String queryDocument() {
        return "";
      }

      @Override public Variables variables() {
        return EMPTY_VARIABLES;
      }

      @Override public ResponseFieldMapper<Data> responseFieldMapper() {
        return new ResponseFieldMapper<Data>() {
          @Override public Data map(ResponseReader responseReader) {
            return null;
          }
        };
      }

      @Nonnull @Override public OperationName name() {
        return operationName;
      }

      @Nonnull @Override public String operationId() {
        return "";
      }

      @Override public Object wrapData(Data data) {
        return data;
      }
    };
  }

  @Test public void httpException() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));

    final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final AtomicReference<String> errorResponse = new AtomicReference<>();
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .doOnError(new Consumer<Throwable>() {
          @Override public void accept(Throwable throwable) throws Exception {
            errorRef.set(throwable);
            errorResponse.set(((ApolloHttpException) throwable).rawResponse().body().string());
          }
        })
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .assertError(ApolloHttpException.class);

    ApolloHttpException e = (ApolloHttpException) errorRef.get();
    assertThat(e.code()).isEqualTo(401);
    assertThat(e.message()).isEqualTo("Client Error");
    assertThat(errorResponse.get()).isEqualTo("Unauthorized request!");
    assertThat(e.getMessage()).isEqualTo("HTTP 401 Client Error");
  }

  @Test public void httpExceptionPrefetch() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized request!"));
    Rx2Apollo
        .from(apolloClient.prefetch(emptyQuery))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(ApolloHttpException.class);
  }

  @Test public void testTimeoutException() throws Exception {
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .test()
        .awaitDone(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(new Predicate<Throwable>() {
          @Override public boolean test(Throwable throwable) throws Exception {
            ApolloNetworkException e = (ApolloNetworkException) throwable;
            assertThat(e.getMessage()).isEqualTo("Failed to execute http call");
            assertThat(e.getCause().getClass()).isEqualTo(SocketTimeoutException.class);
            return true;
          }
        });
  }

  @Test public void testTimeoutExceptionPrefetch() throws Exception {
    Rx2Apollo
        .from(apolloClient.prefetch(emptyQuery))
        .test()
        .awaitDone(TIMEOUT_SECONDS * 2, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(new Predicate<Throwable>() {
          @Override public boolean test(Throwable throwable) throws Exception {
            ApolloNetworkException e = (ApolloNetworkException) throwable;
            assertThat(e.getMessage()).isEqualTo("Failed to execute http call");
            assertThat(e.getCause().getClass()).isEqualTo(SocketTimeoutException.class);
            return true;
          }
        });
  }

  @Test public void testParseException() throws Exception {
    server.enqueue(new MockResponse().setBody("Noise"));
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(new Predicate<Throwable>() {
          @Override public boolean test(Throwable throwable) throws Exception {
            ApolloParseException e = (ApolloParseException) throwable;
            assertThat(e.getMessage()).isEqualTo("Failed to parse http response");
            assertThat(e.getCause().getClass()).isEqualTo(JsonEncodingException.class);
            return true;
          }
        });
  }
}
