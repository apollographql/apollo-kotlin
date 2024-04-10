
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.FooSubscription
import com.apollographql.apollo3.testing.ackMessage
import com.apollographql.apollo3.testing.internal.runTest
import com.apollographql.apollo3.testing.operationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.Test

class SampleServerCustomTest {
  @Test
  fun websocketRetryDoNotPile() = runTest {
    var mockServer = MockServer()
    val port = mockServer.port()

    var reopenCount = 0
    val iterations = 50

    ApolloClient.Builder()
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .serverUrl("https://unused.com/")
        .addRetryOnErrorInterceptor { _, _ ->
          delay(1000)
          reopenCount++
          true
        }
        .build()
        .use { apolloClient ->

          prepareMockServer(mockServer, iterations)
          repeat(iterations) {
            launch {
              println("iteration: $it")
              try {
                withTimeout(20_000) {
                  /**
                   * We're only using the first item of each subscription
                   */
                  apolloClient.subscription(FooSubscription())
                      .toFlow()
                      .onEach {
                        println("received ${it.data}")
                      }
                      /**
                       * Take 2 item:
                       * - first item straight ahead
                       * - second item is after the retry
                       */
                      .take(2)
                      .collect()
                }
              } catch (e: TimeoutCancellationException) {
                /**
                 * Coming here means the retry wasn't able to recover fast enough and means there is an issue somewhere
                 */
                error("timeout")
              }
            }
          }

          delay(1000)
          /**
           * Close the MockServer, retries start kicking in and must not pile
           */
          mockServer.close()
          delay(10_000)
          /**
           * Reopen the MockServer, the second item for each subscription should be emitted quickly after recovery
           */
          mockServer = MockServer.Builder().port(port = port).build()
          prepareMockServer(mockServer, iterations)

          mockServer.close()
        }
  }

  private fun CoroutineScope.prepareMockServer(mockServer: MockServer, repeat: Int) {
    repeat(repeat) {
      launch {
        val webSocket = mockServer.enqueueWebSocket()
        val webSocketRequest = mockServer.awaitWebSocketRequest()

        // connection_init
        webSocketRequest.awaitMessage()
        webSocket.enqueueMessage(ackMessage())

        val operationId = webSocketRequest.awaitMessage().operationId()
        webSocket.enqueueMessage(FooSubscription.nextMessage(operationId, 42))
      }
    }
  }
}
