package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.IdleResourceCallback;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.rx2.Rx2Apollo;

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

import javax.annotation.Nonnull;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static com.google.common.truth.Truth.assertThat;

public class ApolloCallTrackerTest {
  private static final String SERVER_URL = "http://localhost:1234";
  private static final int TIMEOUT_SECONDS = 2;
  private static final Query EMPTY_QUERY = new Query() {

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

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) {
          return null;
        }
      };
    }

    @Override public Object wrapData(Data data) {
      return data;
    }

    @Nonnull @Override public OperationName name() {
      return operationName;
    }

    @Nonnull @Override public String operationId() {
      return "";
    }
  };

  @Rule public final MockWebServer server = new MockWebServer();
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
  public void testRunningCallsCount_whenSyncPrefetchCallIsMade() throws InterruptedException {
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
    Rx2Apollo
        .from(apolloClient.prefetch(EMPTY_QUERY))
        .test()
        .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertThat(activeCallCounts).isEqualTo(Collections.singletonList(1));
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
  }

  @Test
  public void testRunningCallsCount_whenAsyncPrefetchCallIsMade() throws InterruptedException {
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
  public void testRunningCallsCount_whenAsyncApolloCallIsMade() throws InterruptedException {
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
  public void testIdleCallBackIsInvoked_whenApolloClientBecomesIdle() throws InterruptedException, TimeoutException {
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
