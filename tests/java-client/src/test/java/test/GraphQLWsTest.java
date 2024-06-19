package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.java.client.ApolloCallback;
import com.apollographql.java.client.ApolloClient;
import com.apollographql.java.client.ApolloDisposable;
import com.apollographql.java.client.network.ws.WebSocketNetworkTransport;
import com.apollographql.java.client.network.ws.protocol.GraphQLWsProtocol;
import com.apollographql.java.client.network.ws.protocol.WsProtocol;
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
    Truth.assertThat(response.dataOrThrow().hello).isEqualTo("Hello World!");
  }

  @Test
  public void mutationOverWebSocket() {
    ApolloResponse<SetHelloMutation.Data> response = blockingMutation(apolloClient, SetHelloMutation.builder().build());
    Truth.assertThat(response.dataOrThrow().hello).isEqualTo("Hello Mutation!");
  }

  @Test
  public void subscriptionOverWebSocket() throws Exception {
    CountDownLatch latch = new CountDownLatch(6);

    List<String> actual = new ArrayList<>();
    AtomicBoolean disposed = new AtomicBoolean(false);

    ApolloDisposable disposable = apolloClient.subscription(new GreetingsSubscription()).enqueue(new ApolloCallback<GreetingsSubscription.Data>() {
      @Override
      public void onResponse(@NotNull ApolloResponse<GreetingsSubscription.Data> response) {
        actual.add(response.dataOrThrow().greetings);
        latch.countDown();
      }
    });
    disposable.addListener(() -> {
      latch.countDown();
      disposed.set(true);
    });

    // Long timeout because the server sends a message every 15 seconds
    latch.await(2, TimeUnit.MINUTES);
    Truth.assertThat(actual).containsExactly("Hi", "Bonjour", "Hola", "Ciao", "Zdravo").inOrder();
    Truth.assertThat(disposed.get()).isTrue();
  }
}
