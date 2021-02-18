package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.assertResponse
import com.apollographql.apollo3.Utils.enqueueAndAssertResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.api.Response.Companion.builder
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.integration.normalizer.EpisodeHeroNameQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.*
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import io.reactivex.functions.Predicate
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.TimeoutException

class ApolloInterceptorTest {
  private lateinit var client: ApolloClient

  val server = MockWebServer()

  private var okHttpClient: OkHttpClient? = null

  @Before
  fun setup() {
    okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
  }

  @Test
  @Throws(Exception::class)
  fun asyncApplicationInterceptorCanShortCircuitResponses() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val query = createHeroNameQuery()
    val expectedResponse = prepareInterceptorResponse(query)
    val interceptor = createShortcutInterceptor(expectedResponse)
    client = createApolloClient(interceptor)
    Rx2Apollo.from(client.query(query)).test().assertValue(expectedResponse.parsedResponse.get() as Response<EpisodeHeroNameQuery.Data>)
  }

  @Test
  @Throws(Exception::class)
  fun asyncApplicationInterceptorRewritesResponsesFromServer() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val query = createHeroNameQuery()
    val rewrittenResponse = prepareInterceptorResponse(query)
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                  dispatcher: Executor, callBack: CallBack) {
        chain.proceedAsync(request, dispatcher, object : CallBack {
          override fun onResponse(response: InterceptorResponse) {
            callBack.onResponse(rewrittenResponse)
          }

          override fun onFailure(e: ApolloException) {
            throw RuntimeException(e)
          }

          override fun onCompleted() {
            callBack.onCompleted()
          }

          override fun onFetch(sourceType: FetchSourceType) {
            callBack.onFetch(sourceType)
          }
        })
      }

      override fun dispose() {}
    }
    client = createApolloClient(interceptor)
    Rx2Apollo.from(client.query(query)).test().assertValue(rewrittenResponse.parsedResponse.get() as Response<EpisodeHeroNameQuery.Data>)
  }

  @Test
  @Throws(Exception::class)
  fun asyncApplicationInterceptorThrowsApolloException() {
    val message = "ApolloException"
    val query = createHeroNameQuery()
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                  dispatcher: Executor, callBack: CallBack) {
        val apolloException: ApolloException = ApolloParseException(message)
        callBack.onFailure(apolloException)
      }

      override fun dispose() {}
    }
    client = createApolloClient(interceptor)
    Rx2Apollo.from(client.query(query))
        .test()
        .assertError { throwable -> message == throwable.message && throwable is ApolloParseException }
  }

  @Test
  @Throws(TimeoutException::class, InterruptedException::class)
  fun asyncApplicationInterceptorThrowsRuntimeException() {
    val message = "RuntimeException"
    val query = createHeroNameQuery()
    val interceptor: ApolloInterceptor = object : ApolloInterceptor {
      override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                  dispatcher: Executor, callBack: CallBack) {
        dispatcher.execute { throw RuntimeException(message) }
      }

      override fun dispose() {}
    }
    client = createApolloClient(interceptor)
    Rx2Apollo.from(client.query(query)).test().assertError { throwable -> throwable is RuntimeException && message == throwable.message }
  }

  @Test
  @Throws(Exception::class)
  fun applicationInterceptorCanMakeMultipleRequestsToServer() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_CHANGE))
    val query = createHeroNameQuery()
    val interceptor = createChainInterceptor()
    client = createApolloClient(interceptor)
    enqueueAndAssertResponse(
        server,
        FILE_EPISODE_HERO_NAME_WITH_ID,
        client.query(query)
    ) { (_, data) ->
      assertThat(data!!.hero?.name).isEqualTo("Artoo")
      true
    }
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun onShortCircuitingResponseSubsequentInterceptorsAreNotCalled() {
    val query = createHeroNameQuery()
    val expectedResponse = prepareInterceptorResponse(query)
    val firstInterceptor = createShortcutInterceptor(expectedResponse)
    val secondInterceptor = createChainInterceptor()
    client = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient!!)
        .addApplicationInterceptor(firstInterceptor)
        .addApplicationInterceptor(secondInterceptor)
        .build()
    assertResponse(
        client.query(query)
    ) { response ->
      assertThat(expectedResponse.parsedResponse.get()).isEqualTo(response)
    }
  }

  @Test
  @Throws(ApolloException::class, TimeoutException::class, InterruptedException::class, IOException::class)
  fun onApolloCallCanceledAsyncApolloInterceptorIsDisposed() {
    server.enqueue(mockResponse(FILE_EPISODE_HERO_NAME_WITH_ID))
    val query = createHeroNameQuery()
    val interceptor = SpyingApolloInterceptor()
    val testExecutor = Utils.TestExecutor()
    client = createApolloClient(interceptor, testExecutor)
    val apolloCall: ApolloCall<EpisodeHeroNameQuery.Data> = client.query(query)
    apolloCall.enqueue(object : ApolloCall.Callback<EpisodeHeroNameQuery.Data>() {
      override fun onResponse(response: Response<EpisodeHeroNameQuery.Data>) {}
      override fun onFailure(e: ApolloException) {}
    })
    apolloCall.cancel()
    testExecutor.triggerActions()
    Truth.assertThat(interceptor.isDisposed).isTrue()
  }

  private fun createHeroNameQuery(): EpisodeHeroNameQuery {
    return EpisodeHeroNameQuery(episode = Input.present(Episode.EMPIRE))
  }

  private fun createApolloClient(interceptor: ApolloInterceptor, dispatcher: Executor = immediateExecutor()): ApolloClient {
    return ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient!!)
        .addApplicationInterceptor(interceptor)
        .dispatcher(dispatcher)
        .build()
  }

  private fun prepareInterceptorResponse(query: EpisodeHeroNameQuery): InterceptorResponse {
    val request = Request.Builder()
        .url(server.url("/"))
        .build()
    val okHttpResponse = okhttp3.Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("Intercepted")
        .body(ResponseBody.create(MediaType.parse("text/plain; charset=utf-8"), "fakeResponse"))
        .build()
    val apolloResponse = builder<EpisodeHeroNameQuery.Data>(query).build()
    return InterceptorResponse(okHttpResponse, apolloResponse)
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }

  private class SpyingApolloInterceptor : ApolloInterceptor {
    @Volatile
    var isDisposed = false
    override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain, dispatcher: Executor, callBack: CallBack) {
      chain.proceedAsync(request, dispatcher, callBack)
    }

    override fun dispose() {
      isDisposed = true
    }
  }

  companion object {
    private const val FILE_EPISODE_HERO_NAME_WITH_ID = "EpisodeHeroNameResponseWithId.json"
    private const val FILE_EPISODE_HERO_NAME_CHANGE = "EpisodeHeroNameResponseNameChange.json"
    private fun createChainInterceptor(): ApolloInterceptor {
      return object : ApolloInterceptor {
        override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                    dispatcher: Executor, callBack: CallBack) {
          chain.proceedAsync(request, dispatcher, callBack)
        }

        override fun dispose() {}
      }
    }

    private fun createShortcutInterceptor(expectedResponse: InterceptorResponse): ApolloInterceptor {
      return object : ApolloInterceptor {
        override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                    dispatcher: Executor, callBack: CallBack) {
          callBack.onResponse(expectedResponse)
        }

        override fun dispose() {}
      }
    }
  }
}
