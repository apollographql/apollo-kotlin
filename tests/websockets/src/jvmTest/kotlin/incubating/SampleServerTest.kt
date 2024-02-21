package incubating

import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.network.websocket.GeneralErrorServerMessage
import com.apollographql.apollo3.network.websocket.OperationErrorServerMessage
import com.apollographql.apollo3.network.websocket.ServerMessage
import com.apollographql.apollo3.network.websocket.SubscriptionWsProtocol
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.network.websocket.WsProtocol
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
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import sample.server.CountSubscription
import sample.server.GetMessagesQuery
import sample.server.GraphqlAccessErrorSubscription
import sample.server.OperationErrorSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SampleServerTest {
  private lateinit var sampleServer: SampleServer

  @Before
  fun beforeClass() {
    sampleServer = SampleServer()
  }

  @After
  fun afterClass() {
    sampleServer.close()
  }

  private val networkTransportBuilder = WebSocketNetworkTransport.Builder()
      .wsProtocolFactory(SubscriptionWsProtocol.Factory())


  @Test
  fun simple() {
    val apolloClient = ApolloClient.Builder()
        .serverUrl("unused")
        .subscriptionNetworkTransport(
            networkTransportBuilder
                .serverUrl(sampleServer.subscriptionsUrl())
                .build()
        )
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
    val transport = networkTransportBuilder.serverUrl(
        serverUrl = sampleServer.subscriptionsUrl(),
    ).idleTimeoutMillis(
        idleTimeoutMillis = 1000
    ).build()

    val apolloClient = ApolloClient.Builder()
        .networkTransport(transport)
        .build()

    runBlocking {
      withTimeout(500) {
        apolloClient.subscription(CountSubscription(50, 1000)).toFlow().first()
      }

      assertTrue(transport.isConnected.value)
      delay(500)
      withTimeout(1000) {
        transport.isConnected.first { !it }
      }

      delay(1500)
      val number = withTimeout(500) {
        apolloClient.subscription(CountSubscription(50, 0)).toFlow().drop(3).first().data?.count
      }
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
    val transport = networkTransportBuilder.serverUrl(
        serverUrl = sampleServer.subscriptionsUrl(),
    ).idleTimeoutMillis(
        idleTimeoutMillis = 0
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
       * Make sure we disconnect
       */
      withTimeout(500) {
        transport.isConnected.first { !it }
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
      assertEquals("Woops", error)
    }
  }

  private inline fun <reified T> Any?.cast() = this as T

  private object AuthorizationException : Exception()

  class AuthorizationAwareWsProtocol : WsProtocol {
    @Suppress("UNCHECKED_CAST")
    private fun Any?.asMap() = this as? Map<String, Any?>

    private val delegate = SubscriptionWsProtocol.Factory().build()

    override val name: String
      get() = delegate.name

    override suspend fun connectionInit() = delegate.connectionInit()
    override suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>) = delegate.operationStart(request)
    override suspend fun <D : Operation.Data> operationStop(request: ApolloRequest<D>) = delegate.operationStop(request)
    override suspend fun ping() = delegate.ping()
    override suspend fun pong() = delegate.pong()

    override fun parseServerMessage(text: String): ServerMessage {
      val message = delegate.parseServerMessage(text)
      if (message is OperationErrorServerMessage) {
        val isError = message.payload.asMap()?.get("data")?.asMap()?.get("graphqlAccessError") == null
        if (isError) {
          return GeneralErrorServerMessage(AuthorizationException)
        }
      }
      return message
    }

    class Factory : WsProtocol.Factory {
      override fun build(): WsProtocol {
        return AuthorizationAwareWsProtocol()
      }
    }
  }

  @Test
  fun canResumeAfterGraphQLError() {
    var done = false
    val apolloClient = ApolloClient.Builder()
        .serverUrl(sampleServer.graphqlUrl())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .wsProtocolFactory(AuthorizationAwareWsProtocol.Factory())
                .serverUrl(sampleServer.subscriptionsUrl())
                .reopenWhen { e, _ ->
                  (!done && e is AuthorizationException).also {
                    done = true
                  }
                }
                .build()
        )
        .build()

    runBlocking {
      val list = apolloClient.subscription(GraphqlAccessErrorSubscription(1))
          .toFlow()
          .map {
            it.data!!.graphqlAccessError
          }
          .take(2)
          .toList()
      assertEquals(listOf(0, 0), list)

      val messages = apolloClient.query(GetMessagesQuery())
          .execute()
          .dataOrThrow()
          .receivedMessages
          .map {
            Buffer().writeUtf8(it).jsonReader().readAny() as Map<String, Any?>
          }.filter {
            it["type"] == "start"
          }

      assertEquals(2, messages.size)
      val id1 = messages[0]["id"]
      val id2 = messages[1]["id"]
      assertNotNull(id1)
      assertNotNull(id2)
      assertTrue(id1 != id2)

    }
  }
}

