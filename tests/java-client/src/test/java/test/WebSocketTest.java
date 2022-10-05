package test;

import com.apollographql.apollo.sample.server.DefaultApplication;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import com.google.common.truth.Truth;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import javatest.CountSubscription;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;
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
  public void simple() throws InterruptedException {
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

    Iterable<Integer> iterable = Rx3Apollo.flowable(apolloClient.subscription(new CountSubscription(5, 100)), BackpressureStrategy.BUFFER)
        .map(response -> response.data.count)
        .blockingIterable();

    Truth.assertThat(iterable).containsExactly(0, 1, 2, 3, 4).inOrder();
  }

  @Test
  public void interleavedSubscriptions() throws InterruptedException {
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
    ;

    latch.await(30, TimeUnit.SECONDS);
  }
}
