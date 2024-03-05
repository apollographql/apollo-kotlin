
import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.network.ws.SubscriptionWsProtocol
import com.apollographql.apollo3.network.ws.closeConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import sample.server.CloseSocketMutation
import sample.server.CountSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebSocketErrorsTest {
  companion object {
    private lateinit var sampleServer: SampleServer

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      sampleServer = SampleServer()
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      sampleServer.close()
    }
  }

  @Test
  fun connectionErrorEmitsException() = runBlocking {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.subscriptionsUrl())
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                connectionPayload = { mapOf("return" to "error") }
            )
        )
        .build()

    apolloClient.subscription(CountSubscription(to = 100, intervalMillis = 100))
        .toFlow()
        .test {
          val error = awaitItem().exception
          assertIs<ApolloNetworkException>(error)
          assertTrue(error.cause?.message?.contains("Connection error") == true)
          awaitComplete()
        }

    apolloClient.close()
  }

  @Test
  fun socketClosedEmitsException() = runBlocking {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.subscriptionsUrl())
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                // Not all codes are valid. See RFC-6455
                // https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.2
                connectionPayload = { mapOf("return" to "close(3666)") }
            )
        )
        .build()

    apolloClient.subscription(CountSubscription(to = 100, intervalMillis = 100))
        .toFlow()
        .test {
          val error = awaitItem().exception
          assertIs<ApolloNetworkException>(error)
          assertTrue(error.cause?.message?.contains("WebSocket Closed code='3666'") == true)
          awaitComplete()
        }

    apolloClient.close()
  }

  @Test
  fun socketReopensAfterAnError() = runBlocking {
    var connectionInitCount = 0
    var exception: Throwable? = null

    val apolloClient = ApolloClient.Builder()
        .httpServerUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                connectionPayload = {
                  connectionInitCount++
                  mapOf("return" to "success")
                }
            )
        )
        .webSocketReopenWhen { e, _ ->
          exception = e
          // Only retry once
          connectionInitCount == 1
        }
        .build()

    val items = async {
      apolloClient.subscription(CountSubscription(2, 500))
          .toFlow()
          .map {
            it.dataOrThrow().count
          }
          .toList()
    }

    delay(200)

    // Trigger an error
    val response = apolloClient.mutation(CloseSocketMutation()).toFlow().first()
    assertEquals(response.dataOrThrow().closeAllWebSockets, "Closed 1 session(s)")

    /**
     * The subscription should be restarted and complete successfully the second time
     */
    assertEquals(listOf(0, 0, 1), items.await())
    assertEquals(2, connectionInitCount)
    assertIs<ApolloWebSocketClosedException>(exception)
    assertEquals(1011, (exception as ApolloWebSocketClosedException).code)

    apolloClient.close()
  }

  @Test
  fun disposingTheClientClosesTheWebSocket() = runBlocking {
    var apolloClient = ApolloClient.Builder()
        .httpServerUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .build()


    apolloClient.subscription(CountSubscription(2, 0))
        .toFlow()
        .test {
          awaitItem()
          awaitItem()
          awaitComplete()
        }

    println("dispose")
    apolloClient.close()

    apolloClient = ApolloClient.Builder()
        .httpServerUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .build()

    delay(1000)
    val response = apolloClient.mutation(CloseSocketMutation()).execute()

    println(response.dataOrThrow())
  }

  @Test
  fun flowThrowsIfNoReconnect() = runBlocking {
    val apolloClient = ApolloClient.Builder()
        .httpServerUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                connectionPayload = {
                  mapOf("return" to "success")
                }
            )
        )
        .build()

    launch {
      delay(200)
      apolloClient.mutation(CloseSocketMutation()).execute()
    }

    apolloClient.subscription(CountSubscription(2, 500))
        .toFlow()
        .test {
          awaitItem()
          var exception: Throwable? = awaitItem().exception
          assertIs<ApolloNetworkException>(exception)
          exception = exception.cause
          assertIs<ApolloWebSocketClosedException>(exception)
          assertEquals(1011, exception.code)

          cancelAndIgnoreRemainingEvents()
        }

    apolloClient.close()
  }

  @Test
  fun closeConnectionReconnectsTheWebSocket() = runBlocking {
    class MyWebSocketReconnectException : Exception()

    var connectionInitCount = 0
    val apolloClient = ApolloClient.Builder()
        .httpServerUrl(sampleServer.graphqlUrl())
        .webSocketServerUrl(sampleServer.subscriptionsUrl())
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                connectionPayload = {
                  connectionInitCount++
                  mapOf("return" to "success")
                }
            )
        )
        .webSocketReopenWhen { e, _ ->
          assertIs<MyWebSocketReconnectException>(e)
          true
        }
        .build()

    apolloClient.subscription(CountSubscription(2, 100))
        .toFlow()
        .test {
          awaitItem() // 0

          apolloClient.subscriptionNetworkTransport.closeConnection(MyWebSocketReconnectException())

          awaitItem() // 0 again since we've re-subscribed
          awaitItem() // 1
          awaitComplete()
        }

    apolloClient.close()

    // connectionInitCount is 2 since we returned true in webSocketReopenWhen
    assertEquals(2, connectionInitCount)
  }
}
