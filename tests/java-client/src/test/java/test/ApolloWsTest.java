package test;

import com.apollographql.apollo.sample.server.SampleServer;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.SubscriptionOperationException;
import com.apollographql.java.client.ApolloCallback;
import com.apollographql.java.client.ApolloClient;
import com.apollographql.java.client.ApolloDisposable;
import com.apollographql.java.client.network.ws.protocol.ApolloWsProtocol;
import com.apollographql.java.rx3.Rx3Apollo;
import com.google.common.truth.Truth;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import javatest.CloseSocketMutation;
import javatest.CountSubscription;
import javatest.OperationErrorSubscription;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static test.Utils.sleep;

public class ApolloWsTest {
  private static SampleServer sampleServer;

  @BeforeClass
  public static void beforeClass() {
    sampleServer = new SampleServer();
  }

  @AfterClass
  public static void afterClass() {
    sampleServer.close();
  }

  private ApolloClient apolloClient = new ApolloClient.Builder()
      .serverUrl(sampleServer.subscriptionsUrl())
      .wsProtocolFactory(new ApolloWsProtocol.Factory())
      .build();

  @Test
  public void simple() throws Exception {
    CountDownLatch latch = new CountDownLatch(6);

    List<Integer> actual = new ArrayList<>();
    final ApolloException[] failure = {null};
    AtomicBoolean disposed = new AtomicBoolean(false);

    ApolloDisposable disposable = apolloClient.subscription(new CountSubscription(5, 100)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        actual.add(response.dataOrThrow().count);
        latch.countDown();
      }
    });
    disposable.addListener(() -> {
      latch.countDown();
      disposed.set(true);
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    Truth.assertThat(actual).containsExactly(0, 1, 2, 3, 4).inOrder();
    Truth.assertThat(failure[0]).isNull();
    Truth.assertThat(disposed.get()).isTrue();
  }

  @Test
  public void simpleWithRx() {
    Rx3Apollo.flowable(apolloClient.subscription(new CountSubscription(5, 100)), BackpressureStrategy.BUFFER)
        .map(response -> response.dataOrThrow().count)
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertValueCount(5)
        .assertValues(0, 1, 2, 3, 4);
  }

  @Test
  public void interleavedSubscriptions() throws Exception {
    List<Integer> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    // Execute 1st subscription, adds even numbers
    apolloClient.subscription(new CountSubscription(4, 2000)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        Integer count = response.dataOrThrow().count;
        items.add(count * 2);
        if (count == 0) {
          sleep(500);
          // Execute 2nd subscription, adds odd numbers
          apolloClient.subscription(new CountSubscription(4, 2000)).enqueue(new ApolloCallback<CountSubscription.Data>() {
            @Override
            public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
              items.add(response.dataOrThrow().count * 2 + 1);
            }
          }).addListener(latch::countDown);
        }
      }
    }).addListener(latch::countDown);

    latch.await(30, TimeUnit.SECONDS);
    Truth.assertThat(items).containsExactly(0, 1, 2, 3, 4, 5, 6, 7).inOrder();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void operationError() throws Exception {
    ApolloException[] failure = {null};
    CountDownLatch latch = new CountDownLatch(1);
    apolloClient.subscription(new OperationErrorSubscription()).enqueue(new ApolloCallback<OperationErrorSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<OperationErrorSubscription.Data> response) {
        failure[0] = response.exception;
        latch.countDown();
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    Truth.assertThat(failure[0]).isInstanceOf(SubscriptionOperationException.class);
    SubscriptionOperationException exception = (SubscriptionOperationException) failure[0];
    Map<String, Object> payload = (Map<String, Object>) exception.getPayload();
    Truth.assertThat(payload.get("message")).isEqualTo("Woops");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void operationErrorWithRx() throws Exception {
    Rx3Apollo.flowable(apolloClient.subscription(new OperationErrorSubscription()), BackpressureStrategy.BUFFER)
        .toList()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertValue(responses -> {
          Map<String, Object> payload = (Map<String, Object>) ((SubscriptionOperationException) responses.get(0).exception).getPayload();
          return payload.get("message").equals("Woops");
        });
  }

  @Test
  public void connectError() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://nonexistant")
        .build();

    ApolloException[] failure = {null};
    CountDownLatch latch = new CountDownLatch(1);
    apolloClient.subscription(new OperationErrorSubscription()).enqueue(new ApolloCallback<OperationErrorSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<OperationErrorSubscription.Data> response) {
        failure[0] = response.exception;
        latch.countDown();
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    Truth.assertThat(failure[0]).isInstanceOf(ApolloNetworkException.class);
  }

  @Test
  public void disposeStopsSubscription() throws Exception {
    List<Integer> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    ApolloDisposable[] disposable = {null};
    disposable[0] = apolloClient.subscription(new CountSubscription(50, 10)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        items.add(response.dataOrThrow().count);
        if (response.dataOrThrow().count == 5) {
          disposable[0].dispose();
          latch.countDown();
        }
      }
    });

    Truth.assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    // Wait a bit to be sure we don't receive any other items after the dispose
    sleep(500);
    Truth.assertThat(items).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
  }

  @Test
  public void networkError() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .wsProtocolFactory(new ApolloWsProtocol.Factory())
        .build();

    List<Integer> items = new ArrayList<>();
    ApolloException[] failure = {null};
    CountDownLatch latch = new CountDownLatch(1);
    ApolloDisposable disposable = apolloClient.subscription(new CountSubscription(50, 500)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        if (response.exception != null) {
          failure[0] = response.exception;
          latch.countDown();
        } else {
          items.add(response.dataOrThrow().count);
          if (response.dataOrThrow().count == 2) {
            // Provoke a network error by closing the websocket
            apolloClient.mutation(new CloseSocketMutation()).enqueue(new ApolloCallback<CloseSocketMutation.Data>() {
              @Override
              public void onResponse(@NotNull ApolloResponse<CloseSocketMutation.Data> response) {
              }
            });
          }
        }
      }
    });

    Truth.assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    // Use "at least" because closing the socket takes a little while, and we still receive a few elements during that time
    Truth.assertThat(items).containsAtLeast(0, 1, 2).inOrder();
    // But definitely not the whole list
    Truth.assertThat(items.size()).isLessThan(50);
    Truth.assertThat(failure[0]).isInstanceOf(ApolloNetworkException.class);
    Truth.assertThat(disposable.isDisposed()).isTrue();
  }

  @Test
  public void reopenWhenCloseWebSocket() throws Exception {
    AtomicBoolean hasReopenOccurred = new AtomicBoolean(false);
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .wsProtocolFactory(new ApolloWsProtocol.Factory())
        .wsReopenWhen((throwable, attempt) -> {
          boolean shouldReopen = !hasReopenOccurred.get();
          hasReopenOccurred.set(true);
          return shouldReopen;
        })
        .build();

    List<Integer> itemsBeforeReopen = new ArrayList<>();
    List<Integer> itemsAfterReopen = new ArrayList<>();
    ApolloException[] failure = {null};
    CountDownLatch latch = new CountDownLatch(1);
    ApolloDisposable disposable = apolloClient.subscription(new CountSubscription(50, 500)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        if (response.exception != null) {
          failure[0] = response.exception;
          latch.countDown();
        } else {
          if (hasReopenOccurred.get()) {
            itemsAfterReopen.add(response.dataOrThrow().count);
          } else {
            itemsBeforeReopen.add(response.dataOrThrow().count);
          }
          if (response.dataOrThrow().count == 2) {
            // Provoke a network error by closing the websocket
            apolloClient.mutation(new CloseSocketMutation()).enqueue(new ApolloCallback<CloseSocketMutation.Data>() {
              @Override
              public void onResponse(@NotNull ApolloResponse<CloseSocketMutation.Data> response) {
              }
            });
          }
        }
      }
    });

    Truth.assertThat(latch.await(4, TimeUnit.SECONDS)).isTrue();
    Truth.assertThat(hasReopenOccurred.get()).isTrue();
    // Use "at least" because closing the socket takes a little while, and we still receive a few elements during that time
    Truth.assertThat(itemsBeforeReopen).containsAtLeast(0, 1, 2).inOrder();
    // But definitely not the whole list
    Truth.assertThat(itemsBeforeReopen.size()).isLessThan(50);

    // reopen re-subscribed the subscription, so we should received the items again
    Truth.assertThat(itemsAfterReopen).containsAtLeast(0, 1, 2).inOrder();
    Truth.assertThat(itemsAfterReopen.size()).isLessThan(50);

    Truth.assertThat(failure[0]).isInstanceOf(ApolloNetworkException.class);
    Truth.assertThat(disposable.isDisposed()).isTrue();
  }

  @Test
  public void reopenWhenKillServer() throws Exception {
    // Create a specific server for this test, because we want to kill it
    SampleServer sampleServer2 = new SampleServer();

    AtomicBoolean hasReopenOccurred = new AtomicBoolean(false);
    AtomicLong reopenAttempt = new AtomicLong(0);
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(sampleServer2.subscriptionsUrl())
        .wsProtocolFactory(new ApolloWsProtocol.Factory())
        .wsReopenWhen((throwable, attempt) -> {
          reopenAttempt.set(attempt);
          boolean shouldReopen = !hasReopenOccurred.get();
          hasReopenOccurred.set(true);
          return shouldReopen;
        })
        .build();

    List<Integer> itemsBeforeReopen = new ArrayList<>();
    ApolloException[] failure = {null};
    CountDownLatch latch = new CountDownLatch(1);
    ApolloDisposable disposable = apolloClient.subscription(new CountSubscription(50, 200)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        if (response.exception != null) {
          failure[0] = response.exception;
          latch.countDown();
        } else {
          itemsBeforeReopen.add(response.dataOrThrow().count);
          if (response.dataOrThrow().count == 5) {
            // Provoke a network error by stopping the whole server
            sampleServer2.close();
          }
        }
      }
    });

    latch.await(30, TimeUnit.SECONDS);
    Truth.assertThat(hasReopenOccurred.get()).isTrue();
    // Use "at least" because closing the socket takes a little while, and we still receive a few elements during that time
    Truth.assertThat(itemsBeforeReopen).containsAtLeast(0, 1, 2, 3, 4, 5).inOrder();
    // But definitely not the whole list
    Truth.assertThat(itemsBeforeReopen.size()).isLessThan(50);

    Truth.assertThat(failure[0]).isInstanceOf(ApolloNetworkException.class);
    Truth.assertThat(disposable.isDisposed()).isTrue();
  }

}
