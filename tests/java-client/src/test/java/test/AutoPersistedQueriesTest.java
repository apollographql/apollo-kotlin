package test;

import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.api.http.HttpMethod;
import com.apollographql.apollo3.mockserver.MockRequest;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.rx3.java.Rx3Apollo;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import javatest.GetRandomQuery;
import javatest.PingMutation;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class AutoPersistedQueriesTest {
  MockServer mockServer;
  String url;

  @Before
  public void setup() {
    mockServer = new MockServer();

    /**
     * Because url doesn't suspend on the JVM, we can just use the return value
     */
    url = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });
  }

  @After
  public void tearDown() {
  }

  @Test
  public void withApqsDoesntSendDocument() {

    mockServer.enqueue(
        new MockResponse.Builder()
            .body("{\"data\":{\"random\": 42}}")
            .build()
    );

    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(url)
        .autoPersistedQueries()
        .build();

    Rx3Apollo.flowable(apolloClient.query(new GetRandomQuery()), BackpressureStrategy.BUFFER).blockingFirst();
    MockRequest request = mockServer.takeRequest();

    Assert.assertFalse(request.getBody().utf8().contains("query"));
  }

  @Test
  public void canDisableApqsPerQuery() {

    mockServer.enqueue(
        new MockResponse.Builder()
            .body("{\"data\":{\"random\": 42}}")
            .build()
    );

    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(url)
        .autoPersistedQueries()
        .build();

    Rx3Apollo.flowable(apolloClient.query(
                new GetRandomQuery())
            .enableAutoPersistedQueries(false),
        BackpressureStrategy.BUFFER
    ).blockingFirst();

    MockRequest request = mockServer.takeRequest();

    Assert.assertTrue(request.getMethod().toLowerCase(Locale.ROOT).equals("post"));
    Assert.assertTrue(request.getBody().utf8().contains("query"));
  }

  @Test
  public void withApqsRetriesAfterError() {

    mockServer.enqueue(
        new MockResponse.Builder()
            .body("{\"errors\":[{\"message\": \"PersistedQueryNotFound\"}]}")
            .build()
    );

    mockServer.enqueue(
        new MockResponse.Builder()
            .body("{\"data\":{\"random\": 42}}")
            .build()
    );

    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(url)
        .autoPersistedQueries()
        .build();

    Rx3Apollo.flowable(apolloClient.query(new GetRandomQuery()), BackpressureStrategy.BUFFER).blockingFirst();

    MockRequest request = mockServer.takeRequest();
    Assert.assertFalse(request.getBody().utf8().contains("query"));
    request = mockServer.takeRequest();
    Assert.assertTrue(request.getBody().utf8().contains("query"));
  }

  @Test
  public void mutationsAreSentWithPostRegardlessOfSetting() {

    mockServer.enqueue(
        new MockResponse.Builder()
            .body("{\"errors\":[{\"message\": \"PersistedQueryNotFound\"}]}")
            .build()
    );

    mockServer.enqueue(
        new MockResponse.Builder()
            .body("{\"data\":{\"ping\": true}}")
            .build()
    );
    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(url)
        .autoPersistedQueries(HttpMethod.Get, HttpMethod.Get)
        .build();

    Rx3Apollo.flowable(apolloClient.mutation(new PingMutation()), BackpressureStrategy.BUFFER).blockingFirst();

    MockRequest request = mockServer.takeRequest();
    Assert.assertTrue(request.getMethod().toLowerCase(Locale.ROOT).equals("post"));
    request = mockServer.takeRequest();
    Assert.assertTrue(request.getMethod().toLowerCase(Locale.ROOT).equals("post"));
  }
}
