package test.network

import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.TextMessage
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.FooSubscription.Companion.completeMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.errorMessage
import com.apollographql.apollo3.testing.FooSubscription.Companion.nextMessage
import com.apollographql.apollo3.testing.ackMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.mockServerWebSocketTest
import com.apollographql.apollo3.testing.operationId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WebSocketNetworkTransportTest {
  @Test
  fun simple() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitMessage().operationId()
          repeat(5) {
            serverWriter.enqueueMessage(nextMessage(operationId, it))
            assertEquals(it, awaitItem().data?.foo)
          }
          serverWriter.enqueueMessage(completeMessage(operationId))

          awaitComplete()
        }
  }

  @Test
  fun interleavedSubscriptions() = mockServerWebSocketTest {
    0.until(2).map {
      apolloClient.subscription(FooSubscription()).toFlow()
    }.merge()
        .test {
          awaitConnectionInit()
          val operationId1 = serverReader.awaitMessage().operationId()
          val operationId2 = serverReader.awaitMessage().operationId()

          repeat(3) {
            serverWriter.enqueueMessage(nextMessage(operationId1, it))
            awaitItem().apply {
              assertEquals(operationId1, requestUuid.toString())
              assertEquals(it, data?.foo)
            }
            serverWriter.enqueueMessage(nextMessage(operationId2, it))
            awaitItem().apply {
              assertEquals(operationId2, requestUuid.toString())
              assertEquals(it, data?.foo)
            }
          }

          serverWriter.enqueueMessage(completeMessage(operationId1))
          serverWriter.enqueueMessage(completeMessage(operationId2))

          awaitComplete()
        }
  }

  @Test
  fun slowConsumer() = mockServerWebSocketTest {
    /**
     * Simulate a low read on the first 5 items.
     * During that time, the server should continue sending.
     * Then resume reading as fast as possible and make sure we didn't drop any items.
     */
    /**
     * Simulate a low read on the first 5 items.
     * During that time, the server should continue sending.
     * Then resume reading as fast as possible and make sure we didn't drop any items.
     */
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()
          val operationId = serverReader.awaitMessage().operationId()
          repeat(1000) {
            serverWriter.enqueueMessage(nextMessage(operationId, it))
          }
          delay(3000)
          repeat(1000) {
            assertEquals(it, awaitItem().data?.foo)
          }
          serverWriter.enqueueMessage(completeMessage(operationId))

          awaitComplete()
        }
  }

  @Test
  fun operationError() = mockServerWebSocketTest {
    apolloClient.subscription(FooSubscription())
        .toFlow()
        .test {
          awaitConnectionInit()

          val operationId = serverReader.awaitMessage().operationId()
          serverWriter.enqueueMessage(errorMessage(operationId, "Woops"))
          awaitItem().exception.apply {
            assertIs<SubscriptionOperationException>(this)
            assertEquals("Woops", ((payload as List<*>).first() as Map<*,*>).get("message"))
          }
          awaitComplete()
        }
  }

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