package com.apollographql.apollo.network

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.network.mock.MockHttpResponse
import com.apollographql.apollo.network.mock.MockSessionDataTask
import com.apollographql.apollo.runBlocking
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
import kotlin.test.assertTrue

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
    } catch (e: ApolloException) {
      assertEquals(e.error, ApolloError.Network)
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
    } catch (e: ApolloException) {
      assertTrue(e.error is ApolloError.Http)
      assertEquals(404, (e.error as ApolloError.Http).statusCode)
      assertEquals("header1Value", (e.error as ApolloError.Http).headers["header1"])
      assertEquals("header2Value", (e.error as ApolloError.Http).headers["header2"])
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

    assertEquals("{\"data\":{\"name\":\"MockQuery\"}}", response.body.readUtf8())
    assertNotNull(response.executionContext[HttpExecutionContext.Response])
    assertEquals(200, response.executionContext[HttpExecutionContext.Response]?.statusCode)
    assertEquals("header1Value", response.executionContext[HttpExecutionContext.Response]?.headers?.get("header1"))
    assertEquals("header2Value", response.executionContext[HttpExecutionContext.Response]?.headers?.get("header2"))
  }

  @Test
  fun `when http post, assert request GraphQL request body`() {
    val networkTransport = mockApolloHttpNetworkTransport { request, completionHandler ->
      assertEquals("https://apollo.com", request.URL.toString())
      assertEquals(
          "{\"operationName\":\"TestQuery\",\"query\":\"query { name }\",\"variables\":\"{\\\"key\\\": \\\"value\\\"}\"}",
          request.HTTPBody!!.toByteString().utf8()
      )

      MockSessionDataTask(
          completionHandler = completionHandler,
          mockResponse = mockSuccessHttpResponse()
      )
    }

    runBlocking {
      networkTransport.execute(request = mockGraphQLRequest(), executionContext = ExecutionContext.Empty).single()
    }
  }

  private fun mockApolloHttpNetworkTransport(dataTaskProvider: DataTaskProvider): ApolloHttpNetworkTransport {
    return ApolloHttpNetworkTransport(
        serverUrl = NSURL(string = "https://apollo.com"),
        httpHeaders = emptyMap(),
        httpMethod = HttpMethod.Post,
        dataTaskProvider = dataTaskProvider
    )
  }

  private fun mockGraphQLRequest(): GraphQLRequest {
    return GraphQLRequest(
        operationName = "TestQuery",
        operationId = "123",
        document = "query { name }",
        variables = "{\"key\": \"value\"}"
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
