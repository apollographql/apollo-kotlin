package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.cache.normalized.FetchPolicy;
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory;
import com.apollographql.apollo3.java.ApolloCallback;
import com.apollographql.apollo3.java.ApolloClient;
import com.apollographql.apollo3.java.Subscription;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.google.common.truth.Truth;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class NormalizedCacheTest {
  MockServer mockServer;
  ApolloClient apolloClient;

  @Before
  public void before() {
    mockServer = new MockServer();
    String serverUrl = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });

    NormalizedCacheFactory cacheFactory = new MemoryCacheFactory();
    apolloClient = new ApolloClient.Builder()
        .serverUrl(serverUrl)
        .normalizedCache(cacheFactory)
        .build();

  }

  private void addDataToTheCache(int value) {
    apolloClient.getApolloStore().writeOperation(new GetRandomQuery(), new GetRandomQuery.Data(value));
  }

  @Test
  public void networkOnlyPolicy() {
    addDataToTheCache(42);
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    ApolloResponse<GetRandomQuery.Data> response = apolloClient.query(new GetRandomQuery())
        .fetchPolicy(FetchPolicy.NetworkOnly)
        .executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().random).isEqualTo(43);
  }

  @Test
  public void cacheOnlyPolicy() {
    addDataToTheCache(42);
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    ApolloResponse<GetRandomQuery.Data> response = apolloClient.query(new GetRandomQuery())
        .fetchPolicy(FetchPolicy.CacheOnly)
        .executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().random).isEqualTo(42);
  }

  @Test
  public void cacheFirstPolicy() {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    ApolloResponse<GetRandomQuery.Data> response = apolloClient.query(new GetRandomQuery())
        .fetchPolicy(FetchPolicy.CacheFirst)
        .executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().random).isEqualTo(43);
  }

  @Test
  public void networkFirstPolicy() {
    addDataToTheCache(42);
    ApolloResponse<GetRandomQuery.Data> response = apolloClient.query(new GetRandomQuery())
        .fetchPolicy(FetchPolicy.CacheFirst)
        .executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().random).isEqualTo(42);
  }

  @Test
  public void cacheAndNetworkPolicy() throws Exception {
    addDataToTheCache(42);
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final List<ApolloResponse<GetRandomQuery.Data>> responses = new ArrayList<>();
    int expectedCount = 2;
    CountDownLatch latch = new CountDownLatch(expectedCount);
    apolloClient.query(new GetRandomQuery())
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .execute(new ApolloCallback<GetRandomQuery.Data>() {
          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responses.add(response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(responses.size()).isEqualTo(expectedCount);
    // Cache
    Truth.assertThat(responses.get(0).dataAssertNoErrors().random).isEqualTo(42);
    // Network
    Truth.assertThat(responses.get(1).dataAssertNoErrors().random).isEqualTo(43);
  }

  @Test
  public void executeCacheAndNetwork() throws Exception {
    addDataToTheCache(42);
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final List<ApolloResponse<GetRandomQuery.Data>> responses = new ArrayList<>();
    int expectedCount = 2;
    CountDownLatch latch = new CountDownLatch(expectedCount);
    apolloClient.query(new GetRandomQuery())
        .executeCacheAndNetwork(new ApolloCallback<GetRandomQuery.Data>() {
          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responses.add(response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(responses.size()).isEqualTo(expectedCount);
    // Cache
    Truth.assertThat(responses.get(0).dataAssertNoErrors().random).isEqualTo(42);
    // Network
    Truth.assertThat(responses.get(1).dataAssertNoErrors().random).isEqualTo(43);
  }

  @Test
  public void watch() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final List<ApolloResponse<GetRandomQuery.Data>> responses = new ArrayList<>();
    int expectedCount = 3;
    CountDownLatch latch = new CountDownLatch(expectedCount);
    apolloClient.query(new GetRandomQuery())
        .watch(new ApolloCallback<GetRandomQuery.Data>() {
          private int i = 0;

          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responses.add(response);
            latch.countDown();

            // After receiving the network response, another thread updates the cache twice
            if (i == 0) {
              new Thread(() -> {
                addDataToTheCache(44);
                sleep(100);
                addDataToTheCache(45);
              }).start();
            }
            i++;
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });

    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(responses.size()).isEqualTo(expectedCount);
    // Network
    Truth.assertThat(responses.get(0).dataAssertNoErrors().random).isEqualTo(43);
    // Cache
    Truth.assertThat(responses.get(1).dataAssertNoErrors().random).isEqualTo(44);
    Truth.assertThat(responses.get(2).dataAssertNoErrors().random).isEqualTo(45);
  }

  @Test
  public void watchCloseSubscription() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final List<ApolloResponse<GetRandomQuery.Data>> responses = new ArrayList<>();
    int expectedCount = 3;
    CountDownLatch latch = new CountDownLatch(expectedCount);
    AtomicReference<Subscription> subscription = new AtomicReference<>();
    subscription.set(apolloClient.query(new GetRandomQuery())
        .watch(new ApolloCallback<GetRandomQuery.Data>() {
          private int i = 0;

          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responses.add(response);
            latch.countDown();

            // After receiving the network response, another thread updates the cache twice
            if (i == 0) {
              new Thread(() -> {
                addDataToTheCache(44);
                sleep(100);
                addDataToTheCache(45);
              }).start();
            }

            // After receiving the 2nd response, cancel the subscription
            if (i == 1) {
              subscription.get().cancel();
            }
            i++;
          }

          @Override public void onFailure(Throwable throwable) {
          }
        }));

    boolean timeout = !latch.await(500, TimeUnit.MILLISECONDS);
    // We never received the 3rd response so the latch should timeout
    Truth.assertThat(timeout).isTrue();
    Truth.assertThat(responses.size()).isEqualTo(expectedCount - 1);
    // Network
    Truth.assertThat(responses.get(0).dataAssertNoErrors().random).isEqualTo(43);
    // Cache
    Truth.assertThat(responses.get(1).dataAssertNoErrors().random).isEqualTo(44);
  }

  @Test
  public void watchNoNetwork() throws Exception {
    final List<ApolloResponse<GetRandomQuery.Data>> responses = new ArrayList<>();
    int expectedCount = 2;
    CountDownLatch latch = new CountDownLatch(expectedCount);
    apolloClient.query(new GetRandomQuery())
        .watch(null, new ApolloCallback<GetRandomQuery.Data>() {
          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            System.out.println("onResponse " + response.data.random);
            responses.add(response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });

    // Another thread updates the cache twice
    new Thread(() -> {
      addDataToTheCache(44);
      sleep(100);
      addDataToTheCache(45);
    }).start();

    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(responses.size()).isEqualTo(expectedCount);
    Truth.assertThat(responses.get(0).dataAssertNoErrors().random).isEqualTo(44);
    Truth.assertThat(responses.get(1).dataAssertNoErrors().random).isEqualTo(45);
  }

  private static void sleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException ignored) {
    }
  }

}
