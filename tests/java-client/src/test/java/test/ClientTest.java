package test;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.exception.ApolloException;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.runtime.java.ApolloCall;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.google.common.truth.Truth;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import javatest.CreateCatMutation;
import javatest.GetRandomQuery;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static com.apollographql.apollo3.api.java.Assertions.checkNotNull;

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
    @NonNull ApolloResponse<GetRandomQuery.Data> queryResponse = from(apolloClient.query(GetRandomQuery.builder().build()), BackpressureStrategy.BUFFER).blockingFirst();
    Truth.assertThat(queryResponse.dataAssertNoErrors().random).isEqualTo(42);

    mockServer.enqueue(new MockResponse.Builder().body("{\"data\": {\"createAnimal\": {\"__typename\": \"Cat\", \"species\": \"cat\", \"habitat\": {\"temperature\": 10.5}}}}").build());
    @NonNull ApolloResponse<CreateCatMutation.Data> mutationResponse = from(apolloClient.mutation(CreateCatMutation.builder().build()), BackpressureStrategy.BUFFER).blockingFirst();
    Truth.assertThat(mutationResponse.dataAssertNoErrors().createAnimal.catFragment.species).isEqualTo("cat");
  }

  @NotNull
  @CheckReturnValue
  public static <T extends Operation.Data> Flowable<ApolloResponse<T>> from(@NotNull final ApolloCall<T> call, @NotNull BackpressureStrategy backpressureStrategy) {
    checkNotNull(call, "originalCall == null");
    checkNotNull(backpressureStrategy, "backpressureStrategy == null");
    return Flowable.create(emitter -> {
      ApolloDisposable disposable = call.enqueue(new ApolloCallback<T>() {

        @Override public void onResponse(@NotNull ApolloResponse<T> response) {
          if (!emitter.isCancelled()) {
            emitter.onNext(response);
          }
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          if (!emitter.isCancelled()) {
            emitter.onComplete();
          }
        }
      });

      emitter.setDisposable(new Disposable() {
        @Override public void dispose() {
          disposable.dispose();
        }

        @Override public boolean isDisposed() {
          return disposable.isDisposed();
        }
      });
    }, backpressureStrategy);
  }

}
