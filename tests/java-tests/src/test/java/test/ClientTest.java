package test;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.mockserver.MockServerKt;
import com.apollographql.apollo3.rx2.Rx2Apollo;
import com.google.common.truth.Truth;
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
    String url = (String)mockServer.url(new Continuation<String>() {
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

    ApolloResponse<GetRandomQuery.Data> response = Rx2Apollo.rxSingle(apolloClient.query(new GetRandomQuery())).blockingGet();
    Truth.assertThat(response.dataOrThrow().random).isEqualTo(42);
  }
}
