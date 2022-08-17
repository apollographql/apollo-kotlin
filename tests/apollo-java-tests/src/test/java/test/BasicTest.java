package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.java.ApolloCallback;
import com.apollographql.apollo3.java.ApolloClient;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.google.common.truth.Truth;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BasicTest {
  MockServer mockServer;
  String serverUrl;

  @Before
  public void before() {
    mockServer = new MockServer();
    serverUrl = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });
  }

  @Test
  public void simpleQueryAsyncSuccess() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder().serverUrl(serverUrl).build();
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 42}}").build());
    final AtomicReference<ApolloResponse<GetRandomQuery.Data>> responseRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    apolloClient.query(new GetRandomQuery())
        .addHttpHeader("my-header", "my-value")
        .execute(new ApolloCallback<GetRandomQuery.Data>() {
          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            responseRef.set(response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(responseRef.get().dataAssertNoErrors().random).isEqualTo(42);
    Truth.assertThat(mockServer.takeRequest().getHeaders().get("my-header")).isEqualTo("my-value");
  }

  @Test
  public void simpleQueryAsyncNetworkError() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder().serverUrl("http://nope").build();
    final AtomicReference<Throwable> throwableRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    apolloClient.query(new GetRandomQuery())
        .execute(new ApolloCallback<GetRandomQuery.Data>() {
          @Override public void onResponse(ApolloResponse<GetRandomQuery.Data> response) {
            Assert.fail();
          }

          @Override public void onFailure(Throwable throwable) {
            throwableRef.set(throwable);
            latch.countDown();
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(throwableRef.get()).isInstanceOf(ApolloNetworkException.class);
  }

  @Test
  public void simpleQueryBlockingSuccess() {
    ApolloClient apolloClient = new ApolloClient.Builder().serverUrl(serverUrl).build();
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 42}}").build());
    ApolloResponse<GetRandomQuery.Data> response = apolloClient.query(new GetRandomQuery())
        .addHttpHeader("my-header", "my-value")
        .executeBlocking();
    Truth.assertThat(response.dataAssertNoErrors().random).isEqualTo(42);
    Truth.assertThat(mockServer.takeRequest().getHeaders().get("my-header")).isEqualTo("my-value");
  }

  @Test
  public void simpleQueryBlockingNetworkError() {
    ApolloClient apolloClient = new ApolloClient.Builder().serverUrl("http://nope").build();
    try {
      apolloClient.query(new GetRandomQuery()).executeBlocking();
      Assert.fail("Should throw");
    } catch (Throwable throwable) {
      Truth.assertThat(throwable).isInstanceOf(ApolloNetworkException.class);
    }
  }

  @Test
  public void simpleMutationAsyncSuccess() throws Exception {
    ApolloClient apolloClient = new ApolloClient.Builder().serverUrl(serverUrl).build();
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}").build());
    final AtomicReference<ApolloResponse<CreateCatMutation.Data>> responseRef = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    apolloClient.mutation(new CreateCatMutation())
        .addHttpHeader("my-header", "my-value")
        .execute(new ApolloCallback<CreateCatMutation.Data>() {
          @Override public void onResponse(ApolloResponse<CreateCatMutation.Data> response) {
            responseRef.set(response);
            latch.countDown();
          }

          @Override public void onFailure(Throwable throwable) {
          }
        });
    latch.await(500, TimeUnit.MILLISECONDS);
    Truth.assertThat(responseRef.get().dataAssertNoErrors().createAnimal.catFragment.species).isEqualTo("cat");
    Truth.assertThat(mockServer.takeRequest().getHeaders().get("my-header")).isEqualTo("my-value");
  }

}
