import app.cash.turbine.test
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import test.network.connectionAckMessage
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.assertNoRequest
import com.apollographql.mockserver.awaitWebSocketRequest
import com.apollographql.mockserver.enqueueWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okio.use
import test.FooQuery
import test.FooSubscription
import test.FooSubscription.Companion.completeMessage
import test.FooSubscription.Companion.nextMessage
import test.network.awaitSubscribe
import test.network.enqueueMessage
import test.network.mockServerTest
import test.network.retryWhen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.seconds

class RetryWebSocketsTest {
  @Test
  fun retryIsWorking() = runTest {
    MockServer().use { mockServer ->

      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .retryOnError { it.operation is Subscription }
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(mockServer.url())
                  .build()
          )
          .build().use { apolloClient ->
            apolloClient.subscription(FooSubscription())
                .toFlow()
                .test {
                  val serverWriter = mockServer.enqueueWebSocket(keepAlive = false)
                  var serverReader = mockServer.awaitWebSocketRequest()

                  serverReader.awaitMessage() // connection_init
                  serverWriter.enqueueMessage(connectionAckMessage())

                  val operationId1 = serverReader.awaitSubscribe()
                  serverWriter.enqueueMessage(nextMessage(operationId1, 1))

                  val item1 = awaitItem()
                  assertEquals(1, item1.data?.foo)

                  val serverWriter2 = mockServer.enqueueWebSocket()

                  /**
                   * Close the response body and the TCP socket
                   */
                  serverWriter.close()

                  serverReader = mockServer.awaitWebSocketRequest()

                  serverReader.awaitMessage() // connection_init
                  serverWriter2.enqueueMessage(connectionAckMessage())

                  val operationId2 = serverReader.awaitSubscribe()
                  serverWriter2.enqueueMessage(nextMessage(operationId2, 2))

                  val item2 = awaitItem()
                  assertEquals(2, item2.data?.foo)

                  // The subscriptions MUST use different operationIds
                  assertNotEquals(operationId1, operationId2)

                  serverWriter2.enqueueMessage(completeMessage(operationId2))

                  awaitComplete()
                }
          }
    }
  }

  @Test
  fun socketReopensAfterAnError() = runTest {
    var mockServer = MockServer()

    ApolloClient.Builder()
        .httpServerUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .retryWhen { _, _ ->
          true
        }
        .build()
        .use { apolloClient ->
          var serverWriter = mockServer.enqueueWebSocket()
          apolloClient.subscription(FooSubscription())
              .toFlow()
              .test {
                var serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage()
                serverWriter.enqueueMessage(connectionAckMessage())

                var operationId = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId, 0))

                assertEquals(0, awaitItem().data?.foo)

                /**
                 * Close the server, the flow should restart
                 */
                val port = mockServer.port()
                mockServer.close()

                /**
                 * Looks like Ktor needs some time to close the local address.
                 * Without the delay, I'm getting an "address already in use" error
                 */
                delay(1000)
                mockServer = MockServer.Builder().port(port).build()
                serverWriter = mockServer.enqueueWebSocket()

                serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage()
                serverWriter.enqueueMessage(connectionAckMessage())

                operationId = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId, 1))

                assertEquals(1, awaitItem().data?.foo)

                serverWriter.enqueueMessage(completeMessage(operationId))

                awaitComplete()
              }
        }
  }

  class MyRetryOnErrorInterceptor : ApolloInterceptor {
    data object RetryException : Exception()

    override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
      return chain.proceed(request).onEach {
        if (request.retryOnError == true && it.exception != null && it.exception is ApolloNetworkException) {
          throw RetryException
        }
      }.retryWhen { cause, attempt ->
        cause is RetryException && attempt < 2
      }.catch {
        if (it !is RetryException) throw it
      }
    }
  }

  @Test
  fun customRetryOnErrorInterceptor() = runTest {
    val mockServer = MockServer()
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .retryOnError {
          it.operation is Subscription
        }
        .retryOnErrorInterceptor(MyRetryOnErrorInterceptor())
        .build().use { apolloClient ->
          var serverWriter = mockServer.enqueueWebSocket(keepAlive = false)

          apolloClient.subscription(FooSubscription())
              .toFlow()
              .test {
                /*
                 * We retry 2 times, meaning we expect 3 collections
                 */
                repeat(3) {
                  val serverReader = mockServer.awaitWebSocketRequest()

                  serverReader.awaitMessage() // connection_init
                  serverWriter.enqueueMessage(connectionAckMessage())

                  val operationId = serverReader.awaitSubscribe()
                  serverWriter.enqueueMessage(nextMessage(operationId, it))

                  val item1 = awaitItem()
                  assertEquals(it, item1.data?.foo)

                  val lastServerWriter = serverWriter
                  serverWriter = mockServer.enqueueWebSocket(keepAlive = false)
                  lastServerWriter.close()
                }
                assertFails {
                  // Make sure that no retry is done
                  mockServer.awaitAnyRequest(timeout = 1.seconds)
                }
                serverWriter.close()
                awaitComplete()
              }
        }
  }

  @Test
  fun retryCanBeDisabled() = runTest {
    val mockServer = MockServer()
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .retryOnError { it.operation is Subscription }
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(FooSubscription())
              .retryOnError(false)
              .toFlow()
              .test {
                val serverWriter = mockServer.enqueueWebSocket()
                val serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage() // connection_init
                serverWriter.enqueueMessage(connectionAckMessage())

                val operationId1 = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId1, 1))

                val item1 = awaitItem()
                assertEquals(1, item1.data?.foo)

                /**
                 * Close the server to trigger an exception
                 */
                mockServer.close()

                assertIs<ApolloNetworkException>(awaitItem().exception)
                awaitComplete()
              }
        }
  }

  @Test
  fun subscriptionsAreNotRetriedByDefault() = runTest {
    val mockServer = MockServer()
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .build()
        )
        .build().use { apolloClient ->
          apolloClient.subscription(FooSubscription())
              .toFlow()
              .test {
                val serverWriter = mockServer.enqueueWebSocket()
                val serverReader = mockServer.awaitWebSocketRequest()

                serverReader.awaitMessage() // connection_init
                serverWriter.enqueueMessage(connectionAckMessage())

                val operationId1 = serverReader.awaitSubscribe()
                serverWriter.enqueueMessage(nextMessage(operationId1, 1))

                val item1 = awaitItem()
                assertEquals(1, item1.data?.foo)

                /**
                 * Close the server to trigger an exception
                 */
                mockServer.close()

                assertIs<ApolloNetworkException>(awaitItem().exception)
                awaitComplete()
              }
        }
  }

  @Test
  fun queriesAreNotRetriedWhenRetrySubscriptionsIsTrue() = mockServerTest(
      clientBuilder = {
        retryOnError { it.operation is Subscription }
      },
  ) {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).build())

    apolloClient.query(FooQuery())
        .toFlow()
        .test {
          assertIs<ApolloHttpException>(awaitItem().exception)
          awaitComplete()
        }
  }

  @Test
  fun retriesDoNotPile() = runTest {
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
        .retryWhen { _, _ ->
          reopenCount++
          delay(500)
          true
        }
        .build()
        .use { apolloClient ->
          prepareMockServer(mockServer, iterations)
          val jobs = (1..iterations).map {
            launch {
              /**
               * We're only using the first item of each subscription
               */
              apolloClient.subscription(FooSubscription())
                  .toFlow()
                  /**
                   * Take 2 item:
                   * - first item straight ahead
                   * - second item is after the retry
                   */
                  .take(2)
                  .collect()
            }
          }

          delay(1000)
          /**
           * Close the MockServer, retries start kicking in and must not pile
           */
          mockServer.close()
          /**
           * Wait a bit for retries to happen
           */
          delay(2_000)
          /**
           * Reopen the MockServer, the second item for each subscription should be emitted quickly after recovery.
           */
          mockServer = MockServer.Builder().port(port = port).build()
          prepareMockServer(mockServer, iterations)

          /**
           * I'm putting 20 here to be safe but in practice, this shouldn't take more than ~1s on a M1 laptop, maybe more in CI
           */
          withTimeout(20.seconds) {
            jobs.forEach {
              it.join()
            }
          }
          mockServer.close()
        }
  }

  private fun CoroutineScope.prepareMockServer(mockServer: MockServer, repeat: Int) {
    launch {
      val webSocket = mockServer.enqueueWebSocket()
      val webSocketRequest = mockServer.awaitWebSocketRequest()

      // connection_init
      webSocketRequest.awaitMessage()
      webSocket.enqueueMessage(connectionAckMessage())

      repeat(repeat) {
        val operationId = webSocketRequest.awaitSubscribe(messagesToIgnore = setOf("complete"))
        webSocket.enqueueMessage(nextMessage(operationId, 42))
      }
    }
  }
}
