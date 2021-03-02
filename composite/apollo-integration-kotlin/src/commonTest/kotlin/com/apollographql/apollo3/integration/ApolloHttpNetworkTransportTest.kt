package com.apollographql.apollo3.integration

import HeroNameQuery
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpMethod
import com.apollographql.apollo3.network.http.HttpRequest
import com.apollographql.apollo3.network.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpResponseInfo
import com.apollographql.apollo3.testing.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class ApolloHttpNetworkTransportTest {

  @Test
  fun `when error, assert execute fails`() {
    val networkTransport = mockApolloHttpNetworkTransport {
      throw ApolloNetworkException()
    }

    try {
      runBlocking {
        networkTransport.execute(request = request(), responseAdapterCache = ResponseAdapterCache.DEFAULT).single()
        error("we expected the test to fail")
      }
    } catch (e: ApolloNetworkException) {
      // expected
    }
  }

  @Test
  fun `when http error, assert execute fails`() {
    val networkTransport = mockApolloHttpNetworkTransport {
      HttpResponse(
          statusCode = 404,
          headers =  mapOf(
              "header1" to "header1Value",
              "header2" to "header2Value"
          ),
          body = null
      )
    }

    try {
      runBlocking {
        networkTransport.execute(request = request(), responseAdapterCache = ResponseAdapterCache.DEFAULT).single()
      }
    } catch (e: ApolloHttpException) {
      assertEquals(404, e.statusCode)
      assertEquals("header1Value", e.headers["header1"])
      assertEquals("header2Value", e.headers["header2"])
    }
  }

  @Test
  fun `when http success, assert success`() {
    val networkTransport = mockApolloHttpNetworkTransport {
      HttpResponse(
          statusCode = 200,
          headers = emptyMap(),
          body = Buffer().writeUtf8(fixtureResponse("HeroNameResponse.json"))
      )
    }

    val response = runBlocking {
      networkTransport.execute(request = request(), responseAdapterCache = ResponseAdapterCache.DEFAULT).single()
    }

    assertNotNull(response.data)
    assertNotNull(response.executionContext[HttpResponseInfo])
    assertEquals(200, response.executionContext[HttpResponseInfo]?.statusCode)
  }


  private fun mockApolloHttpNetworkTransport(responseProvider: (HttpRequest) -> HttpResponse): ApolloHttpNetworkTransport {
    return ApolloHttpNetworkTransport(
        serverUrl = "https://apollo.com",
        httpMethod = HttpMethod.Post,
        engine = object : HttpEngine {
          override suspend fun <R> execute(request: HttpRequest, block: (HttpResponse) -> R): R {
            return block(responseProvider(request))
          }
        }
    )
  }

  private fun request(): ApolloRequest<HeroNameQuery.Data> {
    return ApolloRequest(HeroNameQuery())
  }
}
