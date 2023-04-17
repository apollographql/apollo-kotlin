package test;

import batching.GetLaunch2Query;
import batching.GetLaunchQuery;
import com.apollographql.apollo3.api.ApolloAdapter.DataDeserializeContext;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.ScalarAdapters;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo3.mockserver.MockRequest;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.google.common.truth.Truth;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.apollographql.apollo3.api.Adapters.AnyApolloAdapter;
import static com.apollographql.apollo3.api.ExecutionOptions.CAN_BE_BATCHED;
import static org.junit.Assert.fail;
import static test.Utils.sleep;

@SuppressWarnings("unchecked")
public class BatchingTest {
  MockServer mockServer;
  ApolloClient apolloClient;
  private String mockServerUrl;

  @Before
  public void before() {
    mockServer = new MockServer();

    /*
      Because url doesn't suspend on the JVM, we can just use the return value
     */
    mockServerUrl = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });

    apolloClient = new ApolloClient.Builder().serverUrl(mockServerUrl).build();
  }

  @Test
  public void queriesAreBatchedByDefault() throws Exception {
    String response = "[{\"data\":{\"launch\":{\"id\":\"83\"}}},{\"data\":{\"launch\":{\"id\":\"84\"}}}]";
    mockServer.enqueue(new MockResponse.Builder().body(response).build());

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .httpBatching(300, 10, true)
        .build();

    List<String> items = new ArrayList<>();

    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetLaunchQuery()).enqueue(new ApolloCallback<GetLaunchQuery.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunchQuery.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    sleep(50);

    apolloClient.query(new GetLaunch2Query()).enqueue(new ApolloCallback<GetLaunch2Query.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunch2Query.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    Truth.assertThat(items).containsExactly("83", "84");

    MockRequest request = mockServer.takeRequest();
    List<Map<String, Object>> requests = (List<Map<String, Object>>) AnyApolloAdapter.fromJson(new BufferedSourceJsonReader(new Buffer().write(request.getBody())), new DataDeserializeContext(ScalarAdapters.Empty, new HashSet<>(), null));

    Truth.assertThat(requests).hasSize(2);
    Truth.assertThat(requests.get(0).get("operationName")).isEqualTo("GetLaunch");
    Truth.assertThat(requests.get(1).get("operationName")).isEqualTo("GetLaunch2");

    // Only one request must have been sent
    try {
      mockServer.takeRequest();
      fail("Should throw");
    } catch (Exception ignored) {
    }
  }

  @Test
  public void queriesAreNotBatchedIfSubmittedFarApart() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("[{\"data\":{\"launch\":{\"id\":\"83\"}}}]").build());
    mockServer.enqueue(new MockResponse.Builder().body("[{\"data\":{\"launch\":{\"id\":\"84\"}}}]").build());

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .httpBatching(10, 10, true)
        .build();

    List<String> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetLaunchQuery()).enqueue(new ApolloCallback<GetLaunchQuery.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunchQuery.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    sleep(200);

    apolloClient.query(new GetLaunch2Query()).enqueue(new ApolloCallback<GetLaunch2Query.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunch2Query.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

    Truth.assertThat(items).containsExactly("83", "84");

    mockServer.takeRequest();
    mockServer.takeRequest();
  }

  @Test
  public void queriesCanBeOptOutOfBatching() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\":{\"launch\":{\"id\":\"83\"}}}").build());
    mockServer.enqueue(new MockResponse.Builder().body("[{\"data\":{\"launch\":{\"id\":\"84\"}}}]").build());

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .httpBatching(300, 10, true)
        .build();

    List<String> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetLaunchQuery())
        .canBeBatched(false)
        .httpHeaders(Collections.singletonList(new HttpHeader("client0", "0")))
        .enqueue(new ApolloCallback<GetLaunchQuery.Data>() {
          @Override public void onResponse(@NotNull ApolloResponse<GetLaunchQuery.Data> response) {
            synchronized (items) {
              items.add(response.dataOrThrow().launch.id);
            }
            latch.countDown();
          }
        });

    sleep(50);

    apolloClient.query(new GetLaunch2Query()).enqueue(new ApolloCallback<GetLaunch2Query.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunch2Query.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

    Truth.assertThat(items).containsExactly("83", "84");

    mockServer.takeRequest();
    mockServer.takeRequest();
  }

  @Test
  public void queriesCanBeOptInOfBatching() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("[{\"data\":{\"launch\":{\"id\":\"83\"}}},{\"data\":{\"launch\":{\"id\":\"84\"}}}]").build());

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .httpBatching(300, 10, true)
        // Opt out by default
        .canBeBatched(false)
        .build();

    List<String> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetLaunchQuery())
        // Opt in this one
        .canBeBatched(true)
        .enqueue(new ApolloCallback<GetLaunchQuery.Data>() {
          @Override public void onResponse(@NotNull ApolloResponse<GetLaunchQuery.Data> response) {
            synchronized (items) {
              items.add(response.dataOrThrow().launch.id);
            }
            latch.countDown();
          }
        });

    sleep(50);

    apolloClient.query(new GetLaunch2Query())
        // Opt in this one
        .canBeBatched(true)
        .enqueue(new ApolloCallback<GetLaunch2Query.Data>() {
          @Override public void onResponse(@NotNull ApolloResponse<GetLaunch2Query.Data> response) {
            synchronized (items) {
              items.add(response.dataOrThrow().launch.id);
            }
            latch.countDown();
          }
        });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

    Truth.assertThat(items).containsExactly("83", "84");

    MockRequest request = mockServer.takeRequest();
    List<Map<String, Object>> requests = (List<Map<String, Object>>) AnyApolloAdapter.fromJson(new BufferedSourceJsonReader(new Buffer().write(request.getBody())), new DataDeserializeContext(ScalarAdapters.Empty, new HashSet<>(), null));

    Truth.assertThat(requests).hasSize(2);
    Truth.assertThat(requests.get(0).get("operationName")).isEqualTo("GetLaunch");
    Truth.assertThat(requests.get(1).get("operationName")).isEqualTo("GetLaunch2");

    // Only one request must have been sent
    try {
      mockServer.takeRequest();
      fail("Should throw");
    } catch (Exception ignored) {
    }
  }

  @Test
  public void httpHeadersOnQueryAreKept() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("[{\"data\":{\"launch\":{\"id\":\"83\"}}},{\"data\":{\"launch\":{\"id\":\"84\"}}}]").build());

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .httpBatching(300, 10, true)
        .addHttpHeader("client0", "0")
        .addHttpHeader("client1", "1")
        .build();


    List<String> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetLaunchQuery()).enqueue(new ApolloCallback<GetLaunchQuery.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunchQuery.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    sleep(50);

    apolloClient.query(new GetLaunch2Query()).enqueue(new ApolloCallback<GetLaunch2Query.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<GetLaunch2Query.Data> response) {
        synchronized (items) {
          items.add(response.dataOrThrow().launch.id);
        }
        latch.countDown();
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

    Truth.assertThat(items).containsExactly("83", "84");

    MockRequest request = mockServer.takeRequest();
    Truth.assertThat(request.getHeaders().get("client0")).isEqualTo("0");
    Truth.assertThat(request.getHeaders().get("client1")).isEqualTo("1");
    Truth.assertThat(request.getHeaders().get(CAN_BE_BATCHED)).isNull();
  }

  @Test
  public void commonHttpHeadersOnRequestsAreKept() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("[{\"data\":{\"launch\":{\"id\":\"83\"}}},{\"data\":{\"launch\":{\"id\":\"84\"}}}]").build());

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .httpBatching(300, 10, true)
        .build();

    List<String> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetLaunchQuery())
        .canBeBatched(true)
        .addHttpHeader("query1-only", "0")
        .addHttpHeader("query1+query2-same-value", "0")
        .addHttpHeader("query1+query2-different-value", "0")
        .enqueue(new ApolloCallback<GetLaunchQuery.Data>() {
          @Override public void onResponse(@NotNull ApolloResponse<GetLaunchQuery.Data> response) {
            synchronized (items) {
              items.add(response.dataOrThrow().launch.id);
            }
            latch.countDown();
          }
        });

    sleep(50);

    apolloClient.query(new GetLaunch2Query())
        .canBeBatched(true)
        .addHttpHeader("query2-only", "0")
        .addHttpHeader("query1+query2-same-value", "0")
        .addHttpHeader("query1+query2-different-value", "1")
        .enqueue(new ApolloCallback<GetLaunch2Query.Data>() {
          @Override public void onResponse(@NotNull ApolloResponse<GetLaunch2Query.Data> response) {
            synchronized (items) {
              items.add(response.dataOrThrow().launch.id);
            }
            latch.countDown();
          }
        });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

    Truth.assertThat(items).containsExactly("83", "84");

    MockRequest request = mockServer.takeRequest();
    Truth.assertThat(request.getHeaders().get("query1+query2-same-value")).isEqualTo("0");
    Truth.assertThat(request.getHeaders().get("query1-only")).isNull();
    Truth.assertThat(request.getHeaders().get("query2-only")).isNull();
    Truth.assertThat(request.getHeaders().get("query1+query-different-value")).isNull();
    Truth.assertThat(request.getHeaders().get(CAN_BE_BATCHED)).isNull();
  }
}
