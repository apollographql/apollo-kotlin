package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.cache.normalized.FetchPolicy;
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory;
import com.apollographql.apollo3.java.ApolloCall;
import com.apollographql.apollo3.java.ApolloCallback;
import com.apollographql.apollo3.java.ApolloClient;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.google.common.truth.Truth;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": " + value + "}}").build());
    apolloClient.query(new GetRandomQuery()).fetchPolicy(FetchPolicy.NetworkOnly).executeBlocking();
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
    final AtomicReferenceArray<ApolloResponse<GetRandomQuery.Data>> responseRef = new AtomicReferenceArray<>(2);
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetRandomQuery())
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .execute(new ApolloCallback<GetRandomQuery.Data>() {
          private int i = 0;

          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responseRef.set(i++, response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    // Cache
    Truth.assertThat(responseRef.get(0).dataAssertNoErrors().random).isEqualTo(42);
    // Network
    Truth.assertThat(responseRef.get(1).dataAssertNoErrors().random).isEqualTo(43);
  }

  @Test
  public void executeCacheAndNetwork() throws Exception {
    addDataToTheCache(42);
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final AtomicReferenceArray<ApolloResponse<GetRandomQuery.Data>> responseRef = new AtomicReferenceArray<>(2);
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.query(new GetRandomQuery())
        .executeCacheAndNetwork(new ApolloCallback<GetRandomQuery.Data>() {
          private int i = 0;

          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responseRef.set(i++, response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    // Cache
    Truth.assertThat(responseRef.get(0).dataAssertNoErrors().random).isEqualTo(42);
    // Network
    Truth.assertThat(responseRef.get(1).dataAssertNoErrors().random).isEqualTo(43);
  }

  @Test
  public void watch() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final AtomicReferenceArray<ApolloResponse<GetRandomQuery.Data>> responseRef = new AtomicReferenceArray<>(3);
    CountDownLatch latch = new CountDownLatch(3);
    apolloClient.query(new GetRandomQuery())
        .watch(new ApolloCallback<GetRandomQuery.Data>() {
          private int i = 0;

          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responseRef.set(i++, response);
            latch.countDown();

            // After receiving the network response, another thread updates the cache twice
            if (i == 1) {
              new Thread(() -> {
                addDataToTheCache(44);
                addDataToTheCache(45);
              }).start();
            }
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });

    latch.await(500, TimeUnit.MILLISECONDS);
    // Network
    Truth.assertThat(responseRef.get(0).dataAssertNoErrors().random).isEqualTo(43);
    // Cache
    Truth.assertThat(responseRef.get(1).dataAssertNoErrors().random).isEqualTo(44);
    Truth.assertThat(responseRef.get(2).dataAssertNoErrors().random).isEqualTo(45);
  }


  @Test
  public void watchCloseSubscription() throws Exception {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 43}}").build());
    final AtomicReferenceArray<ApolloResponse<GetRandomQuery.Data>> responseRef = new AtomicReferenceArray<>(3);
    CountDownLatch latch = new CountDownLatch(3);
    AtomicReference<ApolloCall.Subscription> subscription = new AtomicReference<>();
    subscription.set(apolloClient.query(new GetRandomQuery())
        .watch(new ApolloCallback<GetRandomQuery.Data>() {
          private int i = 0;

          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responseRef.set(i++, response);
            latch.countDown();

            // After receiving the network response, another thread updates the cache twice
            if (i == 1) {
              new Thread(() -> {
                addDataToTheCache(44);
                addDataToTheCache(45);
              }).start();
            }

            // After receiving the 2nd response, cancel the subscription
            if (i == 2) {
              subscription.get().cancel();
            }
          }

          @Override public void onFailure(Throwable throwable) {
          }
        }));

    boolean timeout = !latch.await(500, TimeUnit.MILLISECONDS);
    // We never received the 3rd response so the latch should timeout
    Truth.assertThat(timeout).isTrue();
    // Network
    Truth.assertThat(responseRef.get(0).dataAssertNoErrors().random).isEqualTo(43);
    // Cache
    Truth.assertThat(responseRef.get(1).dataAssertNoErrors().random).isEqualTo(44);
    Truth.assertThat(responseRef.get(2)).isNull();
  }

}
