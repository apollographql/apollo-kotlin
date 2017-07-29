package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.IdleResourceCallback;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.RealApolloCall;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
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

  private OkHttpClient.Builder okHttpClientBuilder;
  private MockWebServer server;

  @Before
  public void setUp() throws Exception {
    okHttpClientBuilder = new OkHttpClient
        .Builder();
    server = new MockWebServer();
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  public void testRunningCallsCount_whenSyncPrefetchCallIsMade() throws InterruptedException {
    final CountDownLatch firstLatch = new CountDownLatch(1);
    final CountDownLatch secondLatch = new CountDownLatch(1);

    Interceptor interceptor = new Interceptor() {
      @Override public okhttp3.Response intercept(Chain chain) throws IOException {
        try {
          firstLatch.countDown();
          secondLatch.await();
        } catch (InterruptedException e) {
          throw new AssertionError();
        }
        throw new IOException();
      }
    };

    OkHttpClient okHttpClient = okHttpClientBuilder
        .addInterceptor(interceptor)
        .build();

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClient)
        .build();

    ApolloPrefetch prefetch = apolloClient.prefetch(EMPTY_QUERY);
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);

    Thread thread = synchronousPrefetch(prefetch);

    firstLatch.await();
    assertThat(apolloClient.activeCallsCount()).isEqualTo(1);

    secondLatch.countDown();
    thread.join();
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
  }

  @Test
  public void testRunningCallsCount_whenAsyncPrefetchCallIsMade() throws InterruptedException {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClientBuilder.build())
        .build();
    server.enqueue(createMockResponse());

    ApolloPrefetch prefetch = apolloClient.prefetch(EMPTY_QUERY);

    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);

    prefetch.enqueue(new ApolloPrefetch.Callback() {
      @Override public void onSuccess() {
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
      }
    });

    assertThat(apolloClient.activeCallsCount()).isEqualTo(1);
  }

  @Test
  public void testRunningCallsCount_whenSyncApolloCallIsMade() throws InterruptedException {
    final CountDownLatch firstLatch = new CountDownLatch(1);
    final CountDownLatch secondLatch = new CountDownLatch(1);

    ApolloInterceptor interceptor = new ApolloInterceptor() {

      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain) throws ApolloException {
        try {
          firstLatch.countDown();
          secondLatch.await();
        } catch (InterruptedException e) {
          throw new AssertionError();
        }
        throw new ApolloException("ApolloException");
      }

      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {
      }
    };

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClientBuilder.build())
        .addApplicationInterceptor(interceptor)
        .build();

    ApolloCall apolloCall = apolloClient.query(EMPTY_QUERY);
    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);

    Thread thread = synchronousApolloCall(apolloCall);

    firstLatch.await();
    assertThat(apolloClient.activeCallsCount()).isEqualTo(1);

    secondLatch.countDown();
    thread.join();

    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);
  }

  @Test
  public void testRunningCallsCount_whenAsyncApolloCallIsMade() throws InterruptedException {
    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClientBuilder.build())
        .build();
    server.enqueue(createMockResponse());

    RealApolloCall<Object> apolloCall = (RealApolloCall<Object>) apolloClient.query(EMPTY_QUERY);

    assertThat(apolloClient.activeCallsCount()).isEqualTo(0);

    apolloCall.enqueue(new ApolloCall.Callback<Object>() {
      @Override public void onResponse(@Nonnull Response<Object> response) {
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
      }
    });

    assertThat(apolloClient.activeCallsCount()).isEqualTo(1);
  }

  @Test
  public void testIdleCallBackIsInvoked_whenApolloClientBecomesIdle() throws InterruptedException, TimeoutException {
    server.enqueue(createMockResponse());

    final AtomicBoolean idle = new AtomicBoolean();
    final CountDownLatch firstLatch = new CountDownLatch(1);
    final CountDownLatch secondLatch = new CountDownLatch(1);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain) throws ApolloException {
        try {
          secondLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          throw new AssertionError();
        }
        throw new ApolloException("ApolloException");
      }

      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {
      }
    };

    IdleResourceCallback idleResourceCallback = new IdleResourceCallback() {
      @Override public void onIdle() {
        idle.set(true);
      }
    };

    ApolloClient apolloClient = ApolloClient.builder()
        .serverUrl(SERVER_URL)
        .okHttpClient(okHttpClientBuilder.build())
        .addApplicationInterceptor(interceptor)
        .build();
    apolloClient.idleCallback(idleResourceCallback);

    assertThat(idle.get()).isFalse();

    RealApolloCall<Object> apolloCall = (RealApolloCall<Object>) apolloClient.query(EMPTY_QUERY);
    Thread thread = synchronousApolloCall(apolloCall);

    firstLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

    assertThat(idle.get()).isFalse();

    secondLatch.countDown();
    thread.join();
    assertThat(idle.get()).isTrue();
  }

  private Thread synchronousApolloCall(final ApolloCall apolloCall) {
    Runnable runnable = new Runnable() {
      @Override public void run() {
        try {
          apolloCall.execute();
        } catch (ApolloException expected) {
        }
      }
    };
    return startThread(runnable);
  }

  private Thread synchronousPrefetch(final ApolloPrefetch prefetch) {
    Runnable runnable = new Runnable() {
      @Override public void run() {
        try {
          prefetch.execute();
        } catch (ApolloException expected) {
        }
      }
    };
    return startThread(runnable);
  }

  private Thread startThread(Runnable runnable) {
    Thread thread = new Thread(runnable);
    thread.start();
    return thread;
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
