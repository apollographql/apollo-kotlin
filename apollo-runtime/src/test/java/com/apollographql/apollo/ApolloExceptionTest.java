package com.apollographql.apollo;

import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.ResponseReader;
import com.apollographql.apollo.api.internal.json.JsonEncodingException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.rx2.Rx2Apollo;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("unchecked") public class ApolloExceptionTest {
  private static long timeoutSeconds = 2;

  @Rule public final MockWebServer server = new MockWebServer();
  private ApolloClient apolloClient;
  private Query emptyQuery;

  @Before public void setUp() {
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
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

      @NotNull @Override public OperationName name() {
        return operationName;
      }

      @NotNull @Override public String operationId() {
        return "";
      }


      @NotNull @Override public Response parse(@NotNull BufferedSource source) {
        throw new UnsupportedOperationException();
      }

      @NotNull @Override public Response parse(@NotNull BufferedSource source, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
        throw new UnsupportedOperationException();
      }

      @NotNull @Override public Response parse(@NotNull ByteString byteString) {
        throw new UnsupportedOperationException();
      }

      @NotNull @Override public Response parse(@NotNull ByteString byteString, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
        throw new UnsupportedOperationException();
      }

      @NotNull @Override public ByteString composeRequestBody(
          boolean autoPersistQueries,
          boolean withQueryDocument,
          @NotNull ScalarTypeAdapters scalarTypeAdapters
      ) {
        return OperationRequestBodyComposer.compose(this, autoPersistQueries, withQueryDocument, scalarTypeAdapters);
      }

      @NotNull @Override public ByteString composeRequestBody(@NotNull ScalarTypeAdapters scalarTypeAdapters) {
        return OperationRequestBodyComposer.compose(this, false, true, scalarTypeAdapters);
      }

      @NotNull @Override public ByteString composeRequestBody() {
        return OperationRequestBodyComposer.compose(this, false, true, ScalarTypeAdapters.DEFAULT);
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
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
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
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
        .assertNoValues()
        .assertError(ApolloHttpException.class);
  }

  @Test public void testTimeoutException() throws Exception {
    Rx2Apollo
        .from(apolloClient.query(emptyQuery))
        .test()
        .awaitDone(timeoutSeconds * 2, TimeUnit.SECONDS)
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
        .awaitDone(timeoutSeconds * 2, TimeUnit.SECONDS)
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
        .awaitDone(timeoutSeconds, TimeUnit.SECONDS)
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
