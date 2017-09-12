package com.apollographql.apollo;


import android.support.annotation.NonNull;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.integration.normalizer.EpisodeHeroNameQuery;
import com.apollographql.apollo.integration.normalizer.type.Episode;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.FetchOptions;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;

import static com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack;
import static com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse;
import static com.google.common.truth.Truth.assertThat;

public class ApolloInterceptorChainTest {

  @Test
  public void onProceedAsyncCalled_chainPassesControlToInterceptor() throws TimeoutException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(1);

    EpisodeHeroNameQuery query = createQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull CallBack callBack) {
        counter.decrementAndGet();
      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    RealApolloInterceptorChain chain = new RealApolloInterceptorChain(interceptors);
    chain.proceedAsync(new ApolloInterceptor.InterceptorRequest(query, FetchOptions.NETWORK_ONLY, Optional.<Operation.Data>absent()),
        Utils.immediateExecutor(), new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {

          }

          @Override public void onFailure(@Nonnull ApolloException e) {

          }

          @Override public void onCompleted() {

          }

          @Override public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {

          }
        });

    //If counter's count doesn't go down to zero, it means interceptor's interceptAsync wasn't called
    //which means the test should fail.
    if (counter.get() != 0) {
      Assert.fail("Control not passed to the interceptor");
    }
  }

  @Test
  public void onProceedAsyncCalled_correctInterceptorResponseIsReceived() throws TimeoutException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(1);

    EpisodeHeroNameQuery query = createQuery();
    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            callBack.onResponse(expectedResponse);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    RealApolloInterceptorChain chain = new RealApolloInterceptorChain(interceptors);
    chain.proceedAsync(new ApolloInterceptor.InterceptorRequest(query, FetchOptions.NETWORK_ONLY, Optional.<Operation.Data>absent()),
        Utils.immediateExecutor(), new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {
            assertThat(response).isEqualTo(expectedResponse);
            counter.decrementAndGet();
          }

          @Override public void onFailure(@Nonnull ApolloException e) {

          }

          @Override public void onCompleted() {

          }

          @Override public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {

          }
        });

    if (counter.get() != 0) {
      Assert.fail("Interceptor's response not received");
    }
  }

  @Test
  public void onProceedAsyncCalled_correctExceptionIsCaught() throws TimeoutException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(1);

    final String message = "ApolloException";
    EpisodeHeroNameQuery query = createQuery();
    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
          @Nonnull Executor dispatcher, @Nonnull final CallBack callBack) {
        dispatcher.execute(new Runnable() {
          @Override public void run() {
            ApolloException apolloException = new ApolloException(message);
            callBack.onFailure(apolloException);
          }
        });
      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    RealApolloInterceptorChain chain = new RealApolloInterceptorChain(interceptors);
    chain.proceedAsync(new ApolloInterceptor.InterceptorRequest(query, FetchOptions.NETWORK_ONLY, Optional.<Operation.Data>absent()),
        Utils.immediateExecutor(), new CallBack() {
          @Override public void onResponse(@Nonnull InterceptorResponse response) {

          }

          @Override public void onFailure(@Nonnull ApolloException e) {
            assertThat(e.getMessage()).isEqualTo(message);
            counter.decrementAndGet();
          }

          @Override public void onCompleted() {

          }

          @Override public void onFetch(ApolloInterceptor.FetchSourceType sourceType) {

          }
        });

    if (counter.get() != 0) {
      Assert.fail("Exception thrown by Interceptor not caught");
    }
  }

  @Test
  public void onDisposeCalled_interceptorIsDisposed() {
    final AtomicInteger counter = new AtomicInteger(1);
    EpisodeHeroNameQuery query = createQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Override
      public void interceptAsync(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain, @Nonnull
          Executor dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {
        counter.decrementAndGet();
      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    RealApolloInterceptorChain chain = new RealApolloInterceptorChain(interceptors);
    chain.dispose();
    if (counter.get() != 0) {
      Assert.fail("Interceptor's dispose method not called");
    }
  }

  @NonNull
  private EpisodeHeroNameQuery createQuery() {
    return EpisodeHeroNameQuery
        .builder()
        .episode(Episode.EMPIRE)
        .build();
  }

  @NonNull
  private InterceptorResponse prepareInterceptorResponse(EpisodeHeroNameQuery query) {
    Request request = new Request.Builder()
        .url("https://localhost:8080/")
        .build();

    okhttp3.Response okHttpResponse = new okhttp3.Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build();

    Response<EpisodeHeroNameQuery.Data> apolloResponse = Response.<EpisodeHeroNameQuery.Data>builder(query).build();

    return new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());
  }
}
