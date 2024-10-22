package legacy

import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.SubscriptionOperationException
import com.apollographql.apollo.network.ws.SubscriptionWsProtocolAdapter
import com.apollographql.apollo.network.ws.WebSocketConnection
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo.network.ws.WsProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import sample.server.CountSubscription
import sample.server.GraphqlAccessErrorSubscription
import sample.server.OperationErrorSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SampleServerTest {
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
  fun simple() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.subscriptionsUrl())
        .build()

    runBlocking {
      val list = apolloClient.subscription(CountSubscription(5, 0))
          .toFlow()
          .map {
            it.data?.count
          }
          .toList()
      assertEquals(0.until(5).toList(), list)
    }
  }

  @Test
  fun interleavedSubscriptions() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.subscriptionsUrl())
        .build()

    runBlocking {
      val items = mutableListOf<Int>()
      launch {
        apolloClient.subscription(CountSubscription(5, 1000))
            .toFlow()
            .collect {
              items.add(it.data!!.count * 2)
            }
      }
      delay(500)
      apolloClient.subscription(CountSubscription(5, 1000))
          .toFlow()
          .collect {
            items.add(2 * it.data!!.count + 1)
          }
      assertEquals(0.until(10).toList(), items)
    }
  }

  @Test
  fun idleTimeout() {
    val transport = WebSocketNetworkTransport.Builder().serverUrl(
        serverUrl = sampleServer.subscriptionsUrl(),
    ).idleTimeoutMillis(
        idleTimeoutMillis = 1000
    ).build()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(transport)
        .build()

    runBlocking {
      apolloClient.subscription(CountSubscription(50, 1000)).toFlow().first()

      withTimeout(500) {
        transport.subscriptionCount.first { it == 0 }
      }

      delay(1500)
      val number = apolloClient.subscription(CountSubscription(50, 0)).toFlow().drop(3).first().data?.count
      assertEquals(3, number)
    }
  }

  @Test
  fun slowConsumer() {
    val apolloClient = ApolloClient.Builder().serverUrl(serverUrl = sampleServer.subscriptionsUrl()).build()

    runBlocking {
      /**
       * Take 3 items, delaying the first items by 100ms in total.
       * During that time, the server should continue sending. Then resume reading as fast as we can
       * (which is still probably slower than the server) and make sure we didn't drop any items
       */
      val number = apolloClient.subscription(CountSubscription(1000, 0))
          .toFlow()
          .map { it.data!!.count }
          .onEach {
            if (it < 3) {
              delay(100)
            }
          }
          .drop(500)
          .first()

      assertEquals(500, number)
    }
  }

  @Test
  fun serverTermination() {
    val transport = WebSocketNetworkTransport.Builder().serverUrl(
        serverUrl = sampleServer.subscriptionsUrl(),
    ).idleTimeoutMillis(
        idleTimeoutMillis = 1000
    ).build()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(transport)
        .build()
    runBlocking {
      /**
       * Collect all items the server sends us
       */
      apolloClient.subscription(CountSubscription(50, 0)).toFlow().toList()

      /**
       * Make sure we're unsubscribed
       */
      withTimeout(500) {
        transport.subscriptionCount.first { it == 0 }
      }
    }
  }

  @Test
  fun operationError() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.subscriptionsUrl())
        .build()

    runBlocking {
      val response = apolloClient.subscription(OperationErrorSubscription())
          .toFlow()
          .single()
      assertIs<SubscriptionOperationException>(response.exception)
      val error = response.exception.cast<SubscriptionOperationException>().payload
          .cast<Map<String, String>>()
          .get("message")
      assertEquals("Error collecting the source event stream: Woops", error)
    }
  }

  private inline fun <reified T> Any?.cast() = this as T

  private object AuthorizationException : Exception()

  private class AuthorizationAwareWsProtocol(
      webSocketConnection: WebSocketConnection,
      listener: Listener,
  ) : SubscriptionWsProtocolAdapter(webSocketConnection, listener) {
    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap() = this as? Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asList() = this as? List<Any?>

    override fun handleServerMessage(messageMap: Map<String, Any?>) {
      /**
       * For this test, we use the sample server and I haven't figured out a way to make it out errors yet so we just check
       * if the value is null. A more real life example would do something like below
       * val isError = messageMap.get("payload")
       *                      ?.asMap()
       *                      ?.get("errors")
       *                      ?.asList()
       *                      ?.first()
       *                      ?.asMap()
       *                      ?.get("message") == "Unauthorized error"
       */
      val isError = messageMap.get("payload")?.asMap()?.get("data")?.asMap()?.get("graphqlAccessError") == null
      if (isError) {
        /**
         * The server returned a message with an error and no data. Send a general error upstream
         * so that the WebSocket is restarted
         */
        listener.networkError(AuthorizationException)
      } else {
        super.handleServerMessage(messageMap)
      }
    }
  }

  class AuthorizationAwareWsProtocolFactory : WsProtocol.Factory {
    override val name: String
      get() = "graphql-ws"

    override fun create(webSocketConnection: WebSocketConnection, listener: WsProtocol.Listener, scope: CoroutineScope): WsProtocol {
      return AuthorizationAwareWsProtocol(webSocketConnection, listener)
    }
  }

  @Test
  fun canResumeAfterGraphQLError() {
    val wsFactory = AuthorizationAwareWsProtocolFactory()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.subscriptionsUrl())
        .wsProtocol(wsFactory)
        .webSocketReopenWhen { e, _ ->
          e is AuthorizationException
        }
        .build()

    runBlocking {
      val list = apolloClient.subscription(GraphqlAccessErrorSubscription(1))
          .toFlow()
          .map { it.data!!.graphqlAccessError }
          .take(2)
          .toList()
      assertEquals(listOf(0, 0), list)
    }
  }
}