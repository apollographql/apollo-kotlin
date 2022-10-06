package test;

import com.apollographql.apollo.sample.server.DefaultApplication;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.SubscriptionOperationException;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import com.google.common.truth.Truth;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import javatest.CountSubscription;
import javatest.OperationErrorSubscription;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static test.Utils.sleep;

public class WebSocketTest {
  private static ConfigurableApplicationContext context;

  @BeforeClass
  public static void beforeClass() {
    context = SpringApplication.run(DefaultApplication.class);
  }

  @AfterClass
  public static void afterClass() {
    context.close();
  }

  @Test
  public void simple() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build();

    CountDownLatch latch = new CountDownLatch(6);

    List<Integer> actual = new ArrayList<>();
    final ApolloException[] failure = {null};
    AtomicBoolean disposed = new AtomicBoolean(false);

    ApolloDisposable disposable = apolloClient.subscription(new CountSubscription(5, 100)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        actual.add(response.data.count);
        latch.countDown();
      }

      @Override
      public void onFailure(@NotNull ApolloException e) {
        failure[0] = e;
      }
    });
    disposable.addListener(() -> {
      latch.countDown();
      disposed.set(true);
    });

    latch.await(1, TimeUnit.SECONDS);
    Truth.assertThat(actual).containsExactly(0, 1, 2, 3, 4).inOrder();
    Truth.assertThat(failure[0]).isNull();
    Truth.assertThat(disposed.get()).isTrue();
  }

  @Test
  public void simpleWithRx() {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build();

    Rx3Apollo.flowable(apolloClient.subscription(new CountSubscription(5, 100)), BackpressureStrategy.BUFFER)
        .map(response -> response.data.count)
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertValueCount(5)
        .assertValues(0, 1, 2, 3, 4);
  }

  @Test
  public void interleavedSubscriptions() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build();

    List<Integer> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(2);
    apolloClient.subscription(new CountSubscription(5, 1000)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        items.add(response.data.count * 2);
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        throw e;
      }
    }).addListener(latch::countDown);

    sleep(500);

    apolloClient.subscription(new CountSubscription(5, 1000)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        items.add(response.data.count * 2 + 1);
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        throw e;
      }
    }).addListener(latch::countDown);

    latch.await(30, TimeUnit.SECONDS);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void operationError() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build();

    ApolloException[] failure = {null};
    CountDownLatch latch = new CountDownLatch(1);
    apolloClient.subscription(new OperationErrorSubscription()).enqueue(new ApolloCallback<OperationErrorSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<OperationErrorSubscription.Data> response) {
        throw new AssertionError("Should not be called");
      }

      @Override
      public void onFailure(@NotNull ApolloException e) {
        failure[0] = e;
        latch.countDown();
      }
    });

    latch.await(1, TimeUnit.SECONDS);
    Truth.assertThat(failure[0]).isInstanceOf(SubscriptionOperationException.class);
    SubscriptionOperationException exception = (SubscriptionOperationException) failure[0];
    Map<String, Object> payload = (Map<String, Object>) exception.getPayload();
    List<Map<String, String>> errors = (List<Map<String, String>>) payload.get("errors");
    Truth.assertThat(errors.get(0).get("message")).isEqualTo("Woops");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void operationErrorWithRx() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build();

    Rx3Apollo.flowable(apolloClient.subscription(new OperationErrorSubscription()), BackpressureStrategy.BUFFER)
        .map(response -> response)
        .toList()
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertError(e -> {
          Map<String, Object> payload = (Map<String, Object>) ((SubscriptionOperationException) e).getPayload();
          List<Map<String, String>> errors = (List<Map<String, String>>) payload.get("errors");
          return errors.get(0).get("message").equals("Woops");
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
        throw new AssertionError("Should not be called");
      }

      @Override
      public void onFailure(@NotNull ApolloException e) {
        failure[0] = e;
        latch.countDown();
      }
    });

    latch.await(1, TimeUnit.SECONDS);
    Truth.assertThat(failure[0]).isInstanceOf(ApolloNetworkException.class);
  }

  @Test
  public void disposeStopsSubscription() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .build();

    List<Integer> items = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(1);
    ApolloDisposable[] disposable = {null};
    disposable[0] = apolloClient.subscription(new CountSubscription(50, 10)).enqueue(new ApolloCallback<CountSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<CountSubscription.Data> response) {
        items.add(response.data.count);
        if (response.data.count == 5) {
          disposable[0].dispose();
          latch.countDown();
        }
      }

      @Override
      public void onFailure(@NotNull ApolloException e) {
        throw new AssertionError("Should not be called");
      }
    });

    latch.await(1, TimeUnit.SECONDS);
    // Wait a bit to be sure we don't receive any other items after the dispose
    sleep(500);
    Truth.assertThat(items).containsExactly(0, 1, 2, 3, 4, 5).inOrder();
  }
}
