
import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.network.ws.SubscriptionWsProtocol
import com.apollographql.apollo3.network.ws.closeConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import sample.server.CloseSocketQuery
import sample.server.CountSubscription
import sample.server.TimeSubscription
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

    apolloClient.subscription(TimeSubscription())
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

    apolloClient.subscription(TimeSubscription())
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

    /**
     * The timings in the sample-server are a bit weird. It takes 500ms for the query to reach the
     * backend code during which the subscription code hasn't started yet and then suddendly starts
     * just as the WebSocket is going to be closed. If that ever happen to be an issue in CI or in
     * another place, relaxing the timings should be ok
     *
     * 1639070973776: wait...
     * 1639070973986: triggering an error
     * 1639070974311: closing session...
     * 1639070974326: emitting 0
     * 1639070974353: session closed.
     * 1639070974373: emitting 0
     * 1639070974877: emitting 1
     */
    println("${System.currentTimeMillis()}: wait...")
    delay(200)

    println("${System.currentTimeMillis()}: triggering an error")
    // Trigger an error
    val response = apolloClient.query(CloseSocketQuery()).execute()

    assertEquals(response.dataOrThrow().closeWebSocket, "Closed 1 session(s)")

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
    val response = apolloClient.query(CloseSocketQuery()).execute()

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
      apolloClient.query(CloseSocketQuery()).execute()
    }

    apolloClient.subscription(CountSubscription(2, 500))
        .toFlow()
        .map {
          it.dataOrThrow().count
        }
        .test {
          awaitItem()
          val exception = awaitError()
          assertIs<ApolloNetworkException>(exception)
          val cause = exception.cause
          assertIs<ApolloWebSocketClosedException>(cause)
          assertEquals(1011, cause.code)
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
