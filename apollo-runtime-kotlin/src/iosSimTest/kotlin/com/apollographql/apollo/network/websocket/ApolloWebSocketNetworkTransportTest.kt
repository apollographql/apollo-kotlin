package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.network.GraphQLRequest
import com.apollographql.apollo.network.GraphQLResponse
import com.apollographql.apollo.network.mock.NSWebSocketFactoryMock
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import kotlin.test.Test
import kotlin.test.assertEquals

@ApolloExperimental
@ExperimentalCoroutinesApi
class ApolloWebSocketNetworkTransportTest {

  @Test
  fun `when happy path, assert all responses delivered and connection closed`() {
    runBlocking {
      val expectedRequest = GraphQLRequest(
          operationName = "operationName",
          operationId = "operationId",
          document = "subscription { name }",
          variables = ""
      )
      val expectedResponses = listOf(
          "{\"data\":{\"name\":\"MockQuery1\"}}",
          "{\"data\":{\"name\":\"MockQuery2\"}}",
          "{\"data\":{\"name\":\"MockQuery3\"}}"
      )
      val webSocketFactory = NSWebSocketFactoryMock(
          expectedRequest = expectedRequest,
          expectedResponseOnStart = mockGraphQLResponse(
              data = expectedResponses.first(),
              requestUuid = expectedRequest.uuid
          )
      )
      val apolloWebSocketFactory = ApolloWebSocketFactory(
          serverUrl = NSURL(string = "https://apollo.com"),
          headers = emptyMap(),
          webSocketFactory = webSocketFactory
      )
      ApolloWebSocketNetworkTransport(apolloWebSocketFactory).execute(
          request = expectedRequest,
          executionContext = ExecutionContext.Empty
      ).assertInOrder(
          mockWebSocketFactory = webSocketFactory,
          expectedResponses = expectedResponses
      )
    }
  }

  private suspend fun Flow<GraphQLResponse>.assertInOrder(
      mockWebSocketFactory: NSWebSocketFactoryMock,
      expectedResponses: List<String>
  ) {
    withTimeout(5_000) {
      collectIndexed { index, actualResponse ->
        assertEquals(
            expectedResponses[index],
            actualResponse.body.readUtf8()
        )
        if (index + 1 < expectedResponses.size) {
          mockWebSocketFactory.lastSessionWebSocketTask.enqueueResponse(
              mockGraphQLResponse(
                  data = expectedResponses[index + 1],
                  requestUuid = actualResponse.requestUuid
              )
          )
        } else {
          mockWebSocketFactory.lastSessionWebSocketTask.enqueueComplete()
        }
      }
    }
  }

  private fun mockGraphQLResponse(data: String, requestUuid: Uuid): GraphQLResponse {
    return GraphQLResponse(
        requestUuid = requestUuid,
        body = Buffer().write("{\"type\":\"data\", \"id\":\"$requestUuid\", \"payload\":$data}".encodeUtf8()),
        executionContext = ExecutionContext.Empty
    )
  }
}
