package com.apollographql.apollo;


import android.support.annotation.NonNull;

import com.apollographql.android.impl.normalizer.EpisodeHeroName;
import com.apollographql.android.impl.normalizer.type.Episode;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
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

  private static final int TIMEOUT_SECONDS = 2;

  private ApolloInterceptorChain chain;

  @After
  public void tearDown() {
    chain = null;
  }

  @Test
  public void onProceedCalled_chainPassesControlToInterceptor() throws ApolloException, TimeoutException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(1);

    EpisodeHeroName query = createQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        counter.decrementAndGet();
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    chain = new RealApolloInterceptorChain(query, interceptors);

    chain.proceed();

    if (counter.get() != 0) {
      Assert.fail("Control not passed to the interceptor");
    }
  }

  @Test
  public void onProceedAsyncCalled_chainPassesControlToInterceptor() throws TimeoutException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(1);

    EpisodeHeroName query = createQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {
        counter.decrementAndGet();
      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    chain = new RealApolloInterceptorChain(query, interceptors);

    chain.proceedAsync(Utils.immediateExecutorService(), new CallBack() {
      @Override public void onResponse(@Nonnull InterceptorResponse response) {

      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    //If counter's count doesn't go down to zero, it means interceptor's interceptAsync wasn't called
    //which means the test should fail.
    if (counter.get() != 0) {
      Assert.fail("Control not passed to the interceptor");
    }
  }

  @Test
  public void onProceedCalled_correctInterceptorResponseIsReceived() throws ApolloException {

    EpisodeHeroName query = createQuery();

    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return expectedResponse;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    chain = new RealApolloInterceptorChain(query, interceptors);

    InterceptorResponse actualResponse = chain.proceed();

    assertThat(actualResponse).isEqualTo(expectedResponse);
  }

  @Test
  public void onProceedAsyncCalled_correctInterceptorResponseIsReceived() throws TimeoutException, InterruptedException {
    final AtomicInteger counter = new AtomicInteger(1);

    EpisodeHeroName query = createQuery();
    final InterceptorResponse expectedResponse = prepareInterceptorResponse(query);

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
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
    chain = new RealApolloInterceptorChain(query, interceptors);

    chain.proceedAsync(Utils.immediateExecutorService(), new CallBack() {
      @Override public void onResponse(@Nonnull InterceptorResponse response) {
        assertThat(response).isEqualTo(expectedResponse);
        counter.decrementAndGet();
      }

      @Override public void onFailure(@Nonnull ApolloException e) {

      }
    });

    if (counter.get() != 0) {
      Assert.fail("Interceptor's response not received");
    }
  }

  @Test
  public void onProceedCalled_correctExceptionIsCaught() {

    final String message = "ApolloException";
    EpisodeHeroName query = createQuery();

    ApolloInterceptor Interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        throw new ApolloException(message);
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {

      }
    };

    List<ApolloInterceptor> interceptors = new ArrayList<>();
    interceptors.add(Interceptor);

    chain = new RealApolloInterceptorChain(query, interceptors);

    try {
      chain.proceed();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(message);
      assertThat(e).isInstanceOf(ApolloException.class);
    }
  }

  @Test
  public void onProceedAsyncCalled_correctExceptionIsCaught() throws TimeoutException, InterruptedException {

    final AtomicInteger counter = new AtomicInteger(1);

    final String message = "ApolloException";
    EpisodeHeroName query = createQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
          @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
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
    chain = new RealApolloInterceptorChain(query, interceptors);

    chain.proceedAsync(Utils.immediateExecutorService(), new CallBack() {
      @Override public void onResponse(@Nonnull InterceptorResponse response) {

      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        assertThat(e.getMessage()).isEqualTo(message);
        counter.decrementAndGet();
      }
    });

    if (counter.get() != 0) {
      Assert.fail("Exception thrown by Interceptor not caught");
    }
  }

  @Test
  public void onDisposeCalled_interceptorIsDisposed() {

    final AtomicInteger counter = new AtomicInteger(1);
    EpisodeHeroName query = createQuery();

    ApolloInterceptor interceptor = new ApolloInterceptor() {
      @Nonnull @Override
      public InterceptorResponse intercept(Operation operation, ApolloInterceptorChain chain) throws ApolloException {
        return null;
      }

      @Override
      public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain, @Nonnull ExecutorService dispatcher, @Nonnull CallBack callBack) {

      }

      @Override public void dispose() {
        counter.decrementAndGet();
      }
    };

    List<ApolloInterceptor> interceptors = Collections.singletonList(interceptor);
    chain = new RealApolloInterceptorChain(query, interceptors);

    chain.dispose();

    if (counter.get() != 0) {
      Assert.fail("Interceptor's dispose method not called");
    }
  }

  @NonNull
  private EpisodeHeroName createQuery() {
    return EpisodeHeroName
        .builder()
        .episode(Episode.EMPIRE)
        .build();
  }

  @NonNull
  private InterceptorResponse prepareInterceptorResponse(EpisodeHeroName query) {
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

    Response<EpisodeHeroName.Data> apolloResponse = new Response<>(query);

    return new InterceptorResponse(okHttpResponse,
        apolloResponse, Collections.<Record>emptyList());
  }
}
