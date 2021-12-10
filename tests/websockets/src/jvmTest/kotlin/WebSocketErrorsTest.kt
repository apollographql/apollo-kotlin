import app.cash.turbine.test
import com.apollographql.apollo.sample.server.DefaultApplication
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.network.ws.SubscriptionWsProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import sample.server.CloseSocketQuery
import sample.server.CountSubscription
import sample.server.OperationErrorSubscription
import sample.server.TimeSubscription
import java.util.concurrent.Executors
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebSocketErrorsTest {
  companion object {
    private lateinit var context: ConfigurableApplicationContext

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      context = runApplication<DefaultApplication>()
    }

    @AfterClass
    @JvmStatic
    fun afterClass() {
      context.close()
    }
  }

  @Test
  fun connectionErrorThrows() = runBlocking {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                connectionPayload = { mapOf("return" to "error") }
            )
        )
        .build()

    apolloClient.subscription(TimeSubscription())
        .toFlow()
        .test {
          val error = awaitError()
          assertIs<ApolloNetworkException>(error)
          assertTrue(error.cause?.message?.contains("Connection error") == true)
        }

    apolloClient.dispose()
  }

  @Test
  fun socketClosedThrows() = runBlocking {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("http://localhost:8080/subscriptions")
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
          val error = awaitError()
          assertIs<ApolloNetworkException>(error)
          assertTrue(error.cause?.message?.contains("WebSocket Closed code='3666'") == true)
        }

    apolloClient.dispose()
  }

  @Test
  fun socketReconnectsAfterAnError() = runBlocking {
    var connectionInitCount = 0
    var exception: Throwable? = null

    val apolloClient = ApolloClient.Builder()
        .httpServerUrl("http://localhost:8080/graphql")
        .webSocketServerUrl("http://localhost:8080/subscriptions")
        .wsProtocol(
            SubscriptionWsProtocol.Factory(
                connectionPayload = {
                  connectionInitCount++
                  mapOf("return" to "success")
                }
            )
        )
        .webSocketReconnectWhen {
          exception = it
          // Only retry once
          connectionInitCount == 1
        }
        .build()

    val items = async {
      apolloClient.subscription(CountSubscription(2, 500))
          .toFlow()
          .map {
            it.dataAssertNoErrors.count
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

    assertEquals(response.dataAssertNoErrors.closeWebSocket, "Closed 1 session(s)")

    /**
     * The subscription should be restarted and complete successfully the second time
     */
    assertEquals(listOf(0, 0, 1), items.await())
    assertEquals(2, connectionInitCount)
    assertIs<ApolloWebSocketClosedException>(exception)
    assertEquals(1011, (exception as ApolloWebSocketClosedException).code)

    apolloClient.dispose()
  }

  @Test
  fun disposingTheClientClosesTheWebSocket() = runBlocking {
    var apolloClient = ApolloClient.Builder()
        .httpServerUrl("http://localhost:8080/graphql")
        .webSocketServerUrl("http://localhost:8080/subscriptions")
        .build()


    apolloClient.subscription(CountSubscription(2, 0))
        .toFlow()
        .test {
          awaitItem()
          awaitItem()
          awaitComplete()
        }

    println("dispose")
    apolloClient.dispose()

    apolloClient = ApolloClient.Builder()
        .httpServerUrl("http://localhost:8080/graphql")
        .webSocketServerUrl("http://localhost:8080/subscriptions")
        .build()

    delay(1000)
    val response = apolloClient.query(CloseSocketQuery()).execute()

    println(response.dataAssertNoErrors)
  }

  @Test
  fun flowThrowsIfNoReconnect() = runBlocking {
    val apolloClient = ApolloClient.Builder()
        .httpServerUrl("http://localhost:8080/graphql")
        .webSocketServerUrl("http://localhost:8080/subscriptions")
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
          it.dataAssertNoErrors.count
        }
        .test {
          awaitItem()
          val exception = awaitError()
          assertIs<ApolloNetworkException>(exception)
          val cause = exception.cause
          assertIs<ApolloWebSocketClosedException>(cause)
          assertEquals(1011, cause.code)
        }

    apolloClient.dispose()
  }
}