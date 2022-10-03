package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import com.google.common.truth.Truth;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
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
    mockServer = new MockServer();

    /*
      Because url doesn't suspend on the JVM, we can just use the return value
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
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 42}}").build());
    @NonNull ApolloResponse<GetRandomQuery.Data> queryResponse = Rx3Apollo.single(apolloClient.query(GetRandomQuery.builder().build()), BackpressureStrategy.BUFFER).blockingGet();
    Truth.assertThat(queryResponse.dataAssertNoErrors().random).isEqualTo(42);

    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}").build());
    @NonNull ApolloResponse<CreateCatMutation.Data> mutationResponse = Rx3Apollo.single(apolloClient.mutation(CreateCatMutation.builder().build()), BackpressureStrategy.BUFFER).blockingGet();
    Truth.assertThat(mutationResponse.dataAssertNoErrors().createAnimal.catFragment.species).isEqualTo("cat");
  }

}
