package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.runtime.java.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.internal.ws.GraphQLWsProtocol;
import com.apollographql.apollo3.runtime.java.internal.ws.WebSocketNetworkTransport;
import com.apollographql.apollo3.runtime.java.internal.ws.WsProtocol;
import com.google.common.truth.Truth;
import graphqlws.GreetingsSubscription;
import graphqlws.HelloQuery;
import graphqlws.SetHelloMutation;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static test.Utils.blockingMutation;
import static test.Utils.blockingQuery;

// Ignored because it depends on a local server
// See https://github.com/martinbonnin/graphql-ws-server
// These tests take a long time to execute, this is expected
@Ignore
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GraphQLWsTest {
  private ApolloClient apolloClient = new ApolloClient.Builder()
      .networkTransport(
          new WebSocketNetworkTransport(
              new OkHttpClient(),
              new GraphQLWsProtocol.Factory(
                  () -> null,
                  WsProtocol.WsFrameType.Text,
                  null,
                  null,
                  1000,
                  5000
              ),
              "http://localhost:9090/graphql",
              Collections.emptyList(),
              (throwable, attempt) -> false,
              Executors.newCachedThreadPool(),
              1000
          )
      )
      .build();


  @Test
  public void queryOverWebSocket() {
    ApolloResponse<HelloQuery.Data> response = blockingQuery(apolloClient, HelloQuery.builder().build());
    Truth.assertThat(response.dataAssertNoErrors().hello).isEqualTo("Hello World!");
  }

  @Test
  public void mutationOverWebSocket() {
    ApolloResponse<SetHelloMutation.Data> response = blockingMutation(apolloClient, SetHelloMutation.builder().build());
    Truth.assertThat(response.dataAssertNoErrors().hello).isEqualTo("Hello Mutation!");
  }

  @Test
  public void subscriptionOverWebSocket() throws Exception {
    CountDownLatch latch = new CountDownLatch(6);

    List<String> actual = new ArrayList<>();
    final ApolloException[] failure = {null};
    AtomicBoolean disposed = new AtomicBoolean(false);

    ApolloDisposable disposable = apolloClient.subscription(new GreetingsSubscription()).enqueue(new ApolloCallback<GreetingsSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<GreetingsSubscription.Data> response) {
        actual.add(response.dataAssertNoErrors().greetings);
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

    // Long timeout because the server sends a message every 15 seconds
    latch.await(2, TimeUnit.MINUTES);
    Truth.assertThat(actual).containsExactly("Hi", "Bonjour", "Hola", "Ciao", "Zdravo").inOrder();
    Truth.assertThat(failure[0]).isNull();
    Truth.assertThat(disposed.get()).isTrue();
  }
}
