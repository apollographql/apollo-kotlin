package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Response.Companion.builder
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloGenericException
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.*
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.internal.interceptor.RealApolloInterceptorChain
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import okhttp3.*
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class ApolloInterceptorChainTest {
  @Test
  @Throws(TimeoutException::class, InterruptedException::class)
  fun onProceedAsyncCalled_chainPassesControlToInterceptor() {
    val counter = AtomicInteger(1)
    val query = createQuery()
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                  dispatcher: Executor, callBack: CallBack) {
        counter.decrementAndGet()
      }

      override fun dispose() {}
    }
    val interceptors = listOf(interceptor)
    val chain = RealApolloInterceptorChain(interceptors)
    chain.proceedAsync(
        InterceptorRequest.builder(query).fetchFromCache(false).build(), immediateExecutor(),
        object : CallBack {
          override fun onResponse(response: InterceptorResponse) {}
          override fun onFailure(e: ApolloException) {}
          override fun onCompleted() {}
          override fun onFetch(sourceType: FetchSourceType) {}
        })

    //If counter's count doesn't go down to zero, it means interceptor's interceptAsync wasn't called
    //which means the test should fail.
    if (counter.get() != 0) {
      Assert.fail("Control not passed to the interceptor")
    }
  }

  @Test
  @Throws(TimeoutException::class, InterruptedException::class)
  fun onProceedAsyncCalled_correctInterceptorResponseIsReceived() {
    val counter = AtomicInteger(1)
    val query = createQuery()
    val expectedResponse = prepareInterceptorResponse(query)
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                  dispatcher: Executor, callBack: CallBack) {
        dispatcher.execute { callBack.onResponse(expectedResponse) }
      }

      override fun dispose() {}
    }
    val interceptors = listOf(interceptor)
    val chain = RealApolloInterceptorChain(interceptors)
    chain.proceedAsync(InterceptorRequest.builder(query).fetchFromCache(false).build(),
        immediateExecutor(), object : CallBack {
      override fun onResponse(response: InterceptorResponse) {
        Truth.assertThat(response).isEqualTo(expectedResponse)
        counter.decrementAndGet()
      }

      override fun onFailure(e: ApolloException) {}
      override fun onCompleted() {}
      override fun onFetch(sourceType: FetchSourceType) {}
    })
    if (counter.get() != 0) {
      Assert.fail("Interceptor's response not received")
    }
  }

  @Test
  @Throws(TimeoutException::class, InterruptedException::class)
  fun onProceedAsyncCalled_correctExceptionIsCaught() {
    val counter = AtomicInteger(1)
    val message = "ApolloException"
    val query = createQuery()
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                  dispatcher: Executor, callBack: CallBack) {
        dispatcher.execute {
          val apolloException = ApolloGenericException(message)
          callBack.onFailure(apolloException)
        }
      }

      override fun dispose() {}
    }
    val interceptors = listOf(interceptor)
    val chain = RealApolloInterceptorChain(interceptors)
    chain.proceedAsync(InterceptorRequest.builder(query).fetchFromCache(false).build(),
        immediateExecutor(), object : CallBack {
      override fun onResponse(response: InterceptorResponse) {}
      override fun onFailure(e: ApolloException) {
        assertThat(e.message).isEqualTo(message)
        counter.decrementAndGet()
      }

      override fun onCompleted() {}
      override fun onFetch(sourceType: FetchSourceType) {}
    })
    if (counter.get() != 0) {
      Assert.fail("Exception thrown by Interceptor not caught")
    }
  }

  @Test
  fun onDisposeCalled_interceptorIsDisposed() {
    val counter = AtomicInteger(1)
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain, dispatcher: Executor, callBack: CallBack) {}
      override fun dispose() {
        counter.decrementAndGet()
      }
    }
    val interceptors = listOf(interceptor)
    val chain = RealApolloInterceptorChain(interceptors)
    chain.dispose()
    if (counter.get() != 0) {
      Assert.fail("Interceptor's dispose method not called")
    }
  }

  private fun createQuery(): EpisodeHeroNameQuery {
    return EpisodeHeroNameQuery(episode = Input.present(Episode.EMPIRE))
  }

  private fun prepareInterceptorResponse(query: EpisodeHeroNameQuery): InterceptorResponse {
    val request = Request.Builder()
        .url("https://localhost:8080/")
        .build()
    val okHttpResponse = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build()
    val apolloResponse = builder<EpisodeHeroNameQuery.Data>(query).build()
    return InterceptorResponse(okHttpResponse, apolloResponse)
  }
}
