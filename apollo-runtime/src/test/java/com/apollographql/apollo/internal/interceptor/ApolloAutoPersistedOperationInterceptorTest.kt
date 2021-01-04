package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Response.Companion.builder
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.interceptor.ApolloAutoPersistedOperationInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest.Companion.builder
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.google.common.truth.Truth
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import java.util.UUID
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ApolloAutoPersistedOperationInterceptorTest {
  private val interceptor = ApolloAutoPersistedOperationInterceptor(ApolloLogger(null), false)
  private val interceptorWithGetMethod = ApolloAutoPersistedOperationInterceptor(ApolloLogger(null), true)
  private val request = builder(MockOperation())
      .autoPersistQueries(true)
      .build()

  @Test
  fun initialRequestWithoutQueryDocument() {
    val chain = Mockito.mock(ApolloInterceptorChain::class.java)
    interceptor.interceptAsync(request, chain, TrampolineExecutor(), Mockito.mock(CallBack::class.java))
    val requestArgumentCaptor = ArgumentCaptor.forClass(InterceptorRequest::class.java)
    Mockito.verify(chain).proceedAsync(requestArgumentCaptor.capture(), Matchers.any(Executor::class.java),
        Matchers.any(CallBack::class.java))
    Truth.assertThat(requestArgumentCaptor.value.sendQueryDocument).isFalse()
    Truth.assertThat(requestArgumentCaptor.value.useHttpGetMethodForQueries).isFalse()
    Truth.assertThat(requestArgumentCaptor.value.autoPersistQueries).isTrue()
  }

  @Test
  fun initialRequestWithGetMethodForPersistedQueries() {
    val chain = Mockito.mock(ApolloInterceptorChain::class.java)
    interceptorWithGetMethod.interceptAsync(request, chain, TrampolineExecutor(), Mockito.mock(CallBack::class.java))
    val requestArgumentCaptor = ArgumentCaptor.forClass(InterceptorRequest::class.java)
    Mockito.verify(chain).proceedAsync(requestArgumentCaptor.capture(), Matchers.any(Executor::class.java),
        Matchers.any(CallBack::class.java))
    Truth.assertThat(requestArgumentCaptor.value.sendQueryDocument).isFalse()
    Truth.assertThat(requestArgumentCaptor.value.useHttpGetMethodForQueries).isTrue()
    Truth.assertThat(requestArgumentCaptor.value.autoPersistQueries).isTrue()
  }

  @Test
  fun onPersistedQueryNotFoundErrorRequestWithQueryDocument() {
    val chain: ApolloInterceptorChainAdapter = object : ApolloInterceptorChainAdapter() {
      override fun proceedAsync(request: InterceptorRequest, dispatcher: Executor,
                                callBack: CallBack) {
        super.proceedAsync(request, dispatcher, callBack)
        when (proceedAsyncInvocationCount) {
          1 -> {
            Truth.assertThat(request.sendQueryDocument).isFalse()
            Truth.assertThat(request.autoPersistQueries).isTrue()
            callBack.onResponse(
                InterceptorResponse(
                    mockHttpResponse(),
                    builder<MockOperation.Data>(MockOperation())
                        .errors(listOf(
                            Error("PersistedQueryNotFound", emptyList(), emptyMap<String, Any>())
                        ))
                        .build(), emptyList())
            )
          }
          2 -> {
            Truth.assertThat(request.sendQueryDocument).isTrue()
            Truth.assertThat(request.autoPersistQueries).isTrue()
            callBack.onResponse(
                InterceptorResponse(
                    mockHttpResponse(),
                    builder<MockOperation.Data>(MockOperation())
                        .data(MockOperation.Data())
                        .build(), emptyList())
            )
          }
          else -> {
            Assert.fail("expected only 2 invocation first without query document, second with it")
          }
        }
      }
    }
    val interceptorCallBack = Mockito.mock(CallBack::class.java)
    interceptor.interceptAsync(request, chain, TrampolineExecutor(), interceptorCallBack)
    Truth.assertThat(chain.proceedAsyncInvocationCount).isEqualTo(2)
    val interceptorResponseArgumentCaptor = ArgumentCaptor.forClass(InterceptorResponse::class.java)
    Mockito.verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture())
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.hasErrors()).isFalse()
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.data).isNotNull()
  }

  @Test
  fun onPersistedQueryNotSupportedErrorRequestWithQueryDocument() {
    val chain: ApolloInterceptorChainAdapter = object : ApolloInterceptorChainAdapter() {
      override fun proceedAsync(request: InterceptorRequest, dispatcher: Executor,
                                callBack: CallBack) {
        super.proceedAsync(request, dispatcher, callBack)
        when (proceedAsyncInvocationCount) {
          1 -> {
            Truth.assertThat(request.sendQueryDocument).isFalse()
            Truth.assertThat(request.autoPersistQueries).isTrue()
            callBack.onResponse(
                InterceptorResponse(
                    mockHttpResponse(),
                    builder<MockOperation.Data>(MockOperation())
                        .errors(listOf(
                            Error("PersistedQueryNotSupported", emptyList(), emptyMap<String, Any>())
                        ))
                        .build(), emptyList())
            )
          }
          2 -> {
            Truth.assertThat(request.sendQueryDocument).isTrue()
            Truth.assertThat(request.autoPersistQueries).isTrue()
            callBack.onResponse(
                InterceptorResponse(
                    mockHttpResponse(),
                    builder<MockOperation.Data>(MockOperation())
                        .data(MockOperation.Data())
                        .build(), emptyList())
            )
          }
          else -> {
            Assert.fail("expected only 2 invocation first without query document, second with it")
          }
        }
      }
    }
    val interceptorCallBack = Mockito.mock(CallBack::class.java)
    interceptor.interceptAsync(request, chain, TrampolineExecutor(), interceptorCallBack)
    Truth.assertThat(chain.proceedAsyncInvocationCount).isEqualTo(2)
    val interceptorResponseArgumentCaptor = ArgumentCaptor.forClass(InterceptorResponse::class.java)
    Mockito.verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture())
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.hasErrors()).isFalse()
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.data).isNotNull()
  }

  @Test
  fun onNonPersistedQueryErrorOriginalCallbackCalled() {
    val chain = Mockito.mock(ApolloInterceptorChain::class.java)
    Mockito.doAnswer { invocation ->
      (invocation.arguments[2] as CallBack).onResponse(
          InterceptorResponse(
              mockHttpResponse(),
              builder<MockOperation.Data>(MockOperation())
                  .errors(listOf(
                      Error("SomeOtherError", emptyList(), emptyMap<String, Any>())
                  ))
                  .build(), emptyList())
      )
      null
    }.`when`(chain).proceedAsync(
        Matchers.any(InterceptorRequest::class.java),
        Matchers.any(Executor::class.java),
        Matchers.any(CallBack::class.java)
    )
    val interceptorCallBack = Mockito.mock(CallBack::class.java)
    interceptor.interceptAsync(request, chain, TrampolineExecutor(), interceptorCallBack)
    Mockito.verify(chain).proceedAsync(Matchers.any(InterceptorRequest::class.java), Matchers.any(Executor::class.java),
        Matchers.any(CallBack::class.java))
    val interceptorResponseArgumentCaptor = ArgumentCaptor.forClass(InterceptorResponse::class.java)
    Mockito.verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture())
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.hasErrors()).isTrue()
  }

  @Test
  fun onPersistedQueryFoundCallbackCalled() {
    val chain = Mockito.mock(ApolloInterceptorChain::class.java)
    Mockito.doAnswer { invocation ->
      (invocation.arguments[2] as CallBack).onResponse(
          InterceptorResponse(
              mockHttpResponse(),
              builder<MockOperation.Data>(MockOperation())
                  .data(MockOperation.Data())
                  .build(), emptyList())
      )
      null
    }.`when`(chain).proceedAsync(
        Matchers.any(InterceptorRequest::class.java),
        Matchers.any(Executor::class.java),
        Matchers.any(CallBack::class.java)
    )
    val interceptorCallBack = Mockito.mock(CallBack::class.java)
    interceptor.interceptAsync(request, chain, TrampolineExecutor(), interceptorCallBack)
    Mockito.verify(chain).proceedAsync(Matchers.any(InterceptorRequest::class.java), Matchers.any(Executor::class.java),
        Matchers.any(CallBack::class.java))
    val interceptorResponseArgumentCaptor = ArgumentCaptor.forClass(InterceptorResponse::class.java)
    Mockito.verify(interceptorCallBack).onResponse(interceptorResponseArgumentCaptor.capture())
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.data).isNotNull()
    Truth.assertThat(interceptorResponseArgumentCaptor.value.parsedResponse.get()!!.hasErrors()).isFalse()
  }

  private fun mockHttpResponse(): Response {
    return Response.Builder()
        .request(Request.Builder()
            .url("https://localhost/")
            .build())
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build()
  }

  internal class MockOperation : Operation<MockOperation.Data> {
    override fun queryDocument(): String {
      throw UnsupportedOperationException()
    }

    override fun variables(): Operation.Variables {
      throw UnsupportedOperationException()
    }

    override fun adapter(): ResponseAdapter<Data> {
      throw UnsupportedOperationException()
    }

    override fun name(): OperationName {
      return object : OperationName {
        override fun name(): String {
          return "MockOperation"
        }
      }
    }

    override fun operationId(): String {
      return UUID.randomUUID().toString()
    }

    internal class Data : Operation.Data
  }

  internal class TrampolineExecutor : AbstractExecutorService() {
    override fun shutdown() {}
    override fun shutdownNow(): List<Runnable> {
      return emptyList()
    }

    override fun isShutdown(): Boolean {
      return false
    }

    override fun isTerminated(): Boolean {
      return false
    }

    override fun awaitTermination(l: Long, timeUnit: TimeUnit): Boolean {
      return false
    }

    override fun execute(runnable: Runnable) {
      runnable.run()
    }
  }

  internal open class ApolloInterceptorChainAdapter : ApolloInterceptorChain {
    var proceedAsyncInvocationCount = 0
    override fun proceedAsync(request: InterceptorRequest, dispatcher: Executor,
                              callBack: CallBack) {
      proceedAsyncInvocationCount++
    }

    override fun dispose() {}
  }
}