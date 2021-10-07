package test;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.rx2.Rx2ApolloClient;
import com.google.common.truth.Truth;
import io.reactivex.schedulers.Schedulers;
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

    apolloClient = new ApolloClient(url);
  }

  @Test
  public void simple() {
    mockServer.enqueue(new MockResponse("{\"data\": {\"random\": 42}}"));
    Rx2ApolloClient rx2ApolloClient = new Rx2ApolloClient(apolloClient, Schedulers.io());
    ApolloResponse<GetRandomQuery.Data> response = rx2ApolloClient.query(new GetRandomQuery()).blockingGet();
    Truth.assertThat(response.dataOrThrow().random).isEqualTo(42);
  }
}
