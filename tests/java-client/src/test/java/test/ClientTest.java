package test;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.mockserver.MockRequest;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptorChain;
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

import java.util.Arrays;

public class ClientTest {
  MockServer mockServer;
  ApolloClient apolloClient;
  private String mockServerUrl;

  @Before
  public void before() {
    mockServer = new MockServer();

    /*
      Because url doesn't suspend on the JVM, we can just use the return value
     */
    mockServerUrl = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });

    apolloClient = new ApolloClient.Builder().serverUrl(mockServerUrl).build();
  }

  @Test
  public void simple() {
    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 42}}").build());
    @NonNull ApolloResponse<GetRandomQuery.Data> queryResponse = blockingQuery(GetRandomQuery.builder().build());
    Truth.assertThat(queryResponse.dataAssertNoErrors().random).isEqualTo(42);

    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}").build());
    @NonNull ApolloResponse<CreateCatMutation.Data> mutationResponse = blockingMutation(CreateCatMutation.builder().build());
    Truth.assertThat(mutationResponse.dataAssertNoErrors().createAnimal.catFragment.species).isEqualTo("cat");
  }

  @Test
  public void interceptors() {
    ApolloInterceptor interceptor1 = new ApolloInterceptor() {
      @Override
      public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
        request = request.newBuilder().addHttpHeader("interceptor1", "true").build();
        chain.proceed(request, callback);
      }
    };
    ApolloInterceptor interceptor2 = new ApolloInterceptor() {
      @Override
      public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
        request = request.newBuilder().addHttpHeader("interceptor2", "true").build();
        chain.proceed(request, callback);
      }
    };
    ApolloInterceptor interceptor3 = new ApolloInterceptor() {
      @Override
      public <D extends Operation.Data> void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain chain, @NotNull ApolloCallback<D> callback) {
        request = request.newBuilder().addHttpHeader("interceptor3", "true").build();
        chain.proceed(request, callback);
      }
    };

    apolloClient = new ApolloClient.Builder()
        .serverUrl(mockServerUrl)
        .addInterceptor(interceptor1)
        .addInterceptors(Arrays.asList(interceptor2, interceptor3))
        .build();

    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"random\": 42}}").build());
    @NonNull ApolloResponse<GetRandomQuery.Data> queryResponse = blockingQuery(GetRandomQuery.builder().build());
    MockRequest mockRequest = mockServer.takeRequest();
    Truth.assertThat(mockRequest.getHeaders().get("interceptor1")).isEqualTo("true");
    Truth.assertThat(mockRequest.getHeaders().get("interceptor2")).isEqualTo("true");
    Truth.assertThat(mockRequest.getHeaders().get("interceptor3")).isEqualTo("true");
  }

  @NotNull
  private ApolloResponse<GetRandomQuery.Data> blockingQuery(GetRandomQuery query) {
    return Rx3Apollo.single(apolloClient.query(query), BackpressureStrategy.BUFFER).blockingGet();
  }

  @NotNull
  private ApolloResponse<CreateCatMutation.Data> blockingMutation(CreateCatMutation mutation) {
    return Rx3Apollo.single(apolloClient.mutation(mutation), BackpressureStrategy.BUFFER).blockingGet();
  }

}
