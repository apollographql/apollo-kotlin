package com.apollographql.apollo.network.http

import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.ApolloNetworkException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.testing.MockQuery
import com.apollographql.apollo.network.HttpExecutionContext
import com.apollographql.apollo.network.HttpMethod
import com.apollographql.apollo.network.mock.MockHttpResponse
import com.apollographql.apollo.network.mock.MockSessionDataTask
import com.apollographql.apollo.network.toNSData
import com.apollographql.apollo.testing.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import okio.ByteString.Companion.encodeUtf8
import okio.toByteString
import platform.Foundation.HTTPBody
import platform.Foundation.NSError
import platform.Foundation.NSErrorDomain
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ApolloExperimental
@ExperimentalCoroutinesApi
class ApolloHttpNetworkTransportTest {

  @Test
  fun `when error, assert execute fails`() {
    val networkTransport = mockApolloHttpNetworkTransport { _, completionHandler ->
      MockSessionDataTask(
          completionHandler = completionHandler,
          mockResponse = MockHttpResponse(
              error = NSError(domain = NSErrorDomain(), code = 100, userInfo = emptyMap<Any?, Any?>())
          )
      )
    }

    try {
      runBlocking {
        networkTransport.execute(request = mockGraphQLRequest(), executionContext = ExecutionContext.Empty).single()
      }
    } catch (e: ApolloNetworkException) {
      // expected
    }
  }

  @Test
  fun `when http error, assert execute fails`() {
    val networkTransport = mockApolloHttpNetworkTransport { _, completionHandler ->
      MockSessionDataTask(
          completionHandler = completionHandler,
          mockResponse = MockHttpResponse(
              httpResponse = NSHTTPURLResponse(
                  uRL = NSURL(string = "https://apollo.com"),
                  statusCode = 404,
                  HTTPVersion = null,
                  headerFields = mapOf<Any?, Any>(
                      "header1" to "header1Value",
                      "header2" to "header2Value"
                  )
              )
          )
      )
    }

    try {
      runBlocking {
        networkTransport.execute(request = mockGraphQLRequest(), executionContext = ExecutionContext.Empty).single()
      }
    } catch (e: ApolloHttpException) {
      assertEquals(404, e.statusCode)
      assertEquals("header1Value", e.headers["header1"])
      assertEquals("header2Value", e.headers["header2"])
    }
  }

  @Test
  fun `when http success, assert success`() {
    val networkTransport = mockApolloHttpNetworkTransport { _, completionHandler ->
      MockSessionDataTask(
          completionHandler = completionHandler,
          mockResponse = mockSuccessHttpResponse()
      )
    }

    val response = runBlocking {
      networkTransport.execute(request = mockGraphQLRequest(), executionContext = ExecutionContext.Empty).single()
    }

    assertEquals("{\"data\":{\"name\":\"MockQuery\"}}", response.response.data?.rawResponse)
    assertNotNull(response.executionContext[HttpExecutionContext.Response])
    assertEquals(200, response.executionContext[HttpExecutionContext.Response]?.statusCode)
    assertEquals("header1Value", response.executionContext[HttpExecutionContext.Response]?.headers?.get("header1"))
    assertEquals("header2Value", response.executionContext[HttpExecutionContext.Response]?.headers?.get("header2"))
  }

  @Test
  fun `when http post, assert request GraphQL request body`() {
    val networkTransport = mockApolloHttpNetworkTransport { request, completionHandler ->
      assertEquals("https://apollo.com", request.URL.toString())
      assertEquals(MockQuery().composeRequestBody().utf8(), request.HTTPBody!!.toByteString().utf8())
      MockSessionDataTask(
          completionHandler = completionHandler,
          mockResponse = mockSuccessHttpResponse()
      )
    }

    runBlocking {
      networkTransport.execute(request = mockGraphQLRequest(), executionContext = ExecutionContext.Empty).single()
    }
  }

  private fun mockApolloHttpNetworkTransport(dataTaskFactory: DataTaskFactory): ApolloHttpNetworkTransport {
    return ApolloHttpNetworkTransport(
        serverUrl = NSURL(string = "https://apollo.com"),
        headers = emptyMap(),
        httpMethod = HttpMethod.Post,
        dataTaskFactory = dataTaskFactory,
    )
  }

  private fun mockGraphQLRequest(): ApolloRequest<MockQuery.Data> {
    return ApolloRequest(
        operation = MockQuery(),
        customScalarAdapters = CustomScalarAdapters.DEFAULT,
        executionContext = ExecutionContext.Empty
    )
  }

  private fun mockSuccessHttpResponse(): MockHttpResponse {
    return MockHttpResponse(
        httpResponse = NSHTTPURLResponse(
            uRL = NSURL(string = "https://apollo.com"),
            statusCode = 200L,
            HTTPVersion = null,
            headerFields = mapOf<Any?, Any>(
                "header1" to "header1Value",
                "header2" to "header2Value"
            )
        ),
        httpData = "{\"data\":{\"name\":\"MockQuery\"}}".encodeUtf8().toByteArray().toNSData()
    )
  }
}
