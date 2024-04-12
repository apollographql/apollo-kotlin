package test.network

import app.cash.turbine.test
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeObject
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.TextMessage
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.internal.runTest
import okio.Buffer
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

                  serverWriter.enqueueMessage(TextMessage("{\"type\": \"connection_ack\"}"))
                  val operationId = (serverReader.awaitMessage() as TextMessage).text.readId()

                  serverWriter.enqueueMessage(dataMessage(operationId, 42))
                  assertEquals(42, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(TextMessage("\"Ignored Message\""))
                  expectNoEvents()

                  serverWriter.enqueueMessage(dataMessage(operationId, 41))
                  assertEquals(41, awaitItem().data?.foo)

                  serverWriter.enqueueMessage(completeMessage(operationId))
                  awaitComplete()
                }
          }
    }
  }

  private fun dataMessage(id: String, foo: Int): TextMessage {
    return buildJsonString {
      writeObject {
        name("id")
        value(id)
        name("type")
        value("next")
        name("payload")
        writeObject {
          name("data")
          writeObject {
            name("foo")
            value(foo)
          }
        }
      }
    }.let { TextMessage(it) }
  }

  private fun completeMessage(id: String): TextMessage {
    return buildJsonString {
      writeObject {
        name("id")
        value(id)
        name("type")
        value("complete")
      }
    }.let { TextMessage(it) }
  }


  private fun String.readId(): String {
    return (Buffer().writeUtf8(this).jsonReader().readAny() as Map<*, *>).get("id") as String
  }
}