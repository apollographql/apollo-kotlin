package test.network

import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.TextMessage
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.FooSubscription.Companion.completeMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.nextMessage
import com.apollographql.apollo3.testing.ackMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.operationId
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals

class WebSocketNetworkTransportTest {
  @Test
  fun unknownMessagesDoNotStopTheFlow() = runTest {
    MockServer().use { mockServer ->
      val serverWriter = mockServer.enqueueWebSocket()

      ApolloClient.Builder()
          .serverUrl("unused")
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .build()
          .use { apolloClient ->
            apolloClient.subscription(FooSubscription())
                .toFlow()
                .test {
                  val serverReader = mockServer.awaitWebSocketRequest()
                  serverReader.awaitMessage() // Consume connection_init

                  serverWriter.enqueueMessage(ackMessage())
                  val operationId = serverReader.awaitMessage().operationId()

                  serverWriter.enqueueMessage(nextMessage(operationId, 42))
                  assertEquals(42, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(TextMessage("\"Ignored Message\""))
                  expectNoEvents()

                  serverWriter.enqueueMessage(nextMessage(operationId, 41))
                  assertEquals(41, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(completeMessage(operationId))
                  awaitComplete()
                }
          }
    }
  }

}