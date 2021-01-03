package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.IdleResourceCallback;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller;
import com.apollographql.apollo.api.internal.ResponseReader;
import com.apollographql.apollo.rx2.Rx2Apollo;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;

public class ApolloCallTrackerTest {
  private static final String SERVER_URL = "http://localhost:1234";
  private static final int TIMEOUT_SECONDS = 2;
  private static final Query EMPTY_QUERY = new Query<QueryData, Operation.Variables>() {

    OperationName operationName = new OperationName() {
      @Override public String name() {
        return "EmptyQuery";
      }
    };

    @Override public String queryDocument() {
      return "";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<QueryData> responseFieldMapper() {
      return new ResponseFieldMapper<QueryData>() {
        @Override public QueryData map(ResponseReader responseReader) {
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
  };

  static class QueryData implements Operation.Data {
      @NotNull @Override public ResponseFieldMarshaller marshaller() {
        throw new UnsupportedOperationException();
      }
    }

  @Rule
  public final MockWebServer server = new MockWebServer();
  private List<Integer> activeCallCounts;
  private ApolloClient apolloClient;

  @Before
  public void setUp() throws Exception {
    activeCallCounts = new ArrayList<>();
    Interceptor interceptor = new Interceptor() {
      @Override public okhttp3.Response intercept(Chain chain) throws IOException {
        activeCallCounts.add(apolloClient.activeCallsCount());
        return chain.proceed(chain.request());
      }
    };

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build();

    apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build();
  }

  @Test
  public void testRunningCallsCountWhenSyncPrefetchCallIsMade() throws InterruptedException {
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
    Rx2Apollo
        .from(apolloClient.prefetch(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(activeCallCounts).isEqualTo(Collections.singletonList(1));
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
  }

  @Test
  public void testRunningCallsCountWhenAsyncPrefetchCallIsMade() throws InterruptedException {
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
    server.enqueue(createMockResponse());
    Rx2Apollo
        .from(apolloClient.prefetch(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(activeCallCounts).isEqualTo(Collections.singletonList(1));
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
  }

  @Test
  public void testRunningCallsCountWhenAsyncApolloCallIsMade() throws InterruptedException {
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
    server.enqueue(createMockResponse());
    Rx2Apollo
        .from(apolloClient.query(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(activeCallCounts).isEqualTo(Collections.singletonList(1));
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
  }

  @Test
  public void testIdleCallBackIsInvokedWhenApolloClientBecomesIdle() throws InterruptedException, TimeoutException {
    server.enqueue(createMockResponse());
    final AtomicBoolean idle = new AtomicBoolean();
    IdleResourceCallback idleResourceCallback = new IdleResourceCallback() {
      @Override public void onIdle() {
        idle.set(true);
      }
    };
    apolloClient.idleCallback(idleResourceCallback);

    assertThat(idle.get()).isFalse();
    Rx2Apollo
        .from(apolloClient.query(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(idle.get()).isTrue();
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
