package com.apollographql.apollo

import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Timeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Matchers.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

class DecoratedCallTest {
  private lateinit var apolloClient: ApolloClient
  @get:Rule
  val server = MockWebServer()
  private lateinit var callback: ApolloCall.Callback<AllPlanetsQuery.Data>

  @Before
  fun setup() {
    val okHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()

    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .callFactory(object : Call.Factory {
          override fun newCall(request: Request) = DecoratedCall(okHttpClient.newCall(request))
        })
        .dispatcher(immediateExecutor())
        .build()

    callback = spy(AllPlanetsCallback())
  }

  @Test
  fun onResponseCallbackInvokedWithDecoratedCall() {
    val response = MockResponse().setChunkedBody(readFileToString(javaClass,
        "/" + "HttpCacheTestAllPlanets.json"), 32)
    server.enqueue(response)
    apolloClient.query(AllPlanetsQuery())
        .enqueue(callback)
    verify(callback).onResponse(any())
  }

  @Test
  fun onFailureCallbackInvokedWithDecoratedCall() {
    server.enqueue(MockResponse().setResponseCode(500))
    apolloClient.query(AllPlanetsQuery())
        .enqueue(callback)
    verify(callback).onFailure(any())
  }

  private open class AllPlanetsCallback : ApolloCall.Callback<AllPlanetsQuery.Data>() {
    override fun onResponse(response: Response<AllPlanetsQuery.Data>) { }

    override fun onFailure(e: ApolloException) {}
  }

  private class DecoratedCall internal constructor(private val call: Call) : Call {
    override fun request(): Request = call.request()

    override fun execute(): okhttp3.Response = call.execute()

    override fun enqueue(responseCallback: Callback) = call.enqueue(responseCallback)

    override fun cancel() = call.cancel()

    override fun isExecuted() = call.isExecuted()

    override fun isCanceled() = call.isCanceled()

    override fun timeout(): Timeout = call.timeout()

    override fun clone() = DecoratedCall(call.clone())
  }
}
