package test;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.ApolloClientKt;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.mockserver.MockServerKt;
import com.apollographql.apollo3.network.http.BatchingHttpEngine;
import com.apollographql.apollo3.network.http.BatchingHttpEngineKt;
import com.apollographql.apollo3.network.http.OkHttpEngineKt;
import com.apollographql.apollo3.rx2.Rx2Apollo;
import com.google.common.truth.Truth;
import io.reactivex.disposables.Disposable;
import javatest.AnimalCreatedSubscription;
import javatest.CreateCatMutation;
import javatest.GetRandomQuery;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

public class ClientTest {
  MockServer mockServer;
  ApolloClient apolloClient;

  @Before
  public void before() {
    mockServer = MockServerKt.MockServer();

    /**
     * Because url doesn't suspend on the JVM, we can just use the return value
     */
    String url = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });

    apolloClient = new ApolloClient.Builder().serverUrl(url).build();
  }

  @Test
  public void simple() {
    mockServer.enqueue(new MockResponse("{\"data\": {\"random\": 42}}"));
    ApolloResponse<GetRandomQuery.Data> queryResponse = Rx2Apollo.rxSingle(
        apolloClient.query(new GetRandomQuery())
    ).blockingGet();
    Truth.assertThat(queryResponse.dataAssertNoErrors().random).isEqualTo(42);

    mockServer.enqueue(new MockResponse("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}"));
    ApolloResponse<CreateCatMutation.Data> mutationResponse = Rx2Apollo.rxSingle(
        apolloClient.mutate(new CreateCatMutation())
    ).blockingGet();
    Truth.assertThat(mutationResponse.dataAssertNoErrors().createAnimal.catFragment.species).isEqualTo("cat");

    Disposable disposable = Rx2Apollo.rxFlowable(
        apolloClient.subscribe(new AnimalCreatedSubscription())
    ).subscribe(result -> {
      String species = result.dataAssertNoErrors().animalCreated.catFragment.species;
    });
  }

  private void autoPersistedQueries() {
    apolloClient = ApolloClientKt.autoPersistedQueries(
        new ApolloClient.Builder().serverUrl("https://localhost"),
        HttpMethod.Get,
        HttpMethod.Post,
        true
    ).build();
  }

  private void queryBatching() {
    apolloClient = BatchingHttpEngineKt.canBeBatched(
        new ApolloClient.Builder()
            .serverUrl("https://localhost")
            .httpEngine(
                new BatchingHttpEngine(
                    OkHttpEngineKt.HttpEngine(60_000),
                    10,
                    10
                )
            ),
        false
    ).build();

    ApolloResponse<GetRandomQuery.Data> result = Rx2Apollo.rxSingle(BatchingHttpEngineKt.canBeBatched(
        apolloClient.query(new GetRandomQuery()),
        true
    )).blockingGet();
  }
}
