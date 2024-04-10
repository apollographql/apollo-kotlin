
import app.cash.turbine.test
import com.apollographql.apollo.sample.server.SampleServer
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.exception.SubscriptionConnectionException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.websocket.SubscriptionWsProtocol
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.network.websocket.closeConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import sample.server.CloseSocketMutation
import sample.server.CountSubscription
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun <D : Operation.Data> Flow<ApolloResponse<D>>.retryOnError(block: suspend (ApolloException, Int) -> Boolean): Flow<ApolloResponse<D>> {
  var attempt = 0
  return onEach {
    if (it.exception != null && block(it.exception!!, attempt)) {
      attempt++
      throw RetryException
    }
  }.retryWhen { cause, _ ->
    cause is RetryException
  }
}

class RetryOnErrorInterceptor(private val retryWhen: suspend (ApolloException, Int) -> Boolean) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request).retryOnError(retryWhen)
  }
}

private object RetryException : Exception()

fun ApolloClient.Builder.addRetryOnErrorInterceptor(retryWhen: suspend (ApolloException, Int) -> Boolean) = apply {
  addInterceptor(RetryOnErrorInterceptor(retryWhen))
}

class WebSocketErrorsTest {
  @Test
  fun connectionErrorEmitsException() = runBlocking {
    SampleServer().use { sampleServer ->
      ApolloClient.Builder()
          .serverUrl(sampleServer.subscriptionsUrl())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(sampleServer.subscriptionsUrl())
                  .wsProtocol(SubscriptionWsProtocol { mapOf("return" to "error") })
                  .build()
          )
          .build().use { apolloClient ->
            apolloClient.subscription(CountSubscription(to = 100, intervalMillis = 100))
                .toFlow()
                .test {
                  val error = awaitItem().exception
                  assertIs<SubscriptionConnectionException>(error)
                  assertTrue(error.message?.contains("Subscription connection error") == true)
                  awaitComplete()
                }

          }
    }
  }

  @Test
  fun socketClosedEmitsException() = runBlocking {
    SampleServer().use { sampleServer ->
      ApolloClient.Builder()
          .serverUrl(sampleServer.subscriptionsUrl())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(sampleServer.subscriptionsUrl())
                  .wsProtocol(SubscriptionWsProtocol {
                    // Not all codes are valid. See RFC-6455
                    // https://datatracker.ietf.org/doc/html/rfc6455#section-7.4.2
                    mapOf("return" to "close(3666)")
                  })
                  .build()
          )
          .build().use { apolloClient ->
            apolloClient.subscription(CountSubscription(to = 100, intervalMillis = 100))
                .toFlow()
                .test {
                  val error = awaitItem().exception
                  assertIs<ApolloWebSocketClosedException>(error)
                  assertEquals(3666, error.code)
                  assertEquals("closed", error.reason)
                  awaitComplete()
                }
          }
    }
  }

  @Test
  fun socketReopensAfterAnError(): Unit = runBlocking {
    SampleServer().use { sampleServer ->
      var connectionInitCount = 0
      var exception: Throwable? = null

      ApolloClient.Builder()
          .httpServerUrl(sampleServer.graphqlUrl())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(sampleServer.subscriptionsUrl())
                  .wsProtocol(SubscriptionWsProtocol {
                    connectionInitCount++
                    mapOf("return" to "success")
                  })
                  .build()
          )
          .addRetryOnErrorInterceptor { e, attempt ->
            exception = e
            attempt == 0
          }
          .build()
          .use { apolloClient ->
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
            exception.apply {
              assertIs<ApolloWebSocketClosedException>(this)
              assertEquals(1011, code)
            }
          }
    }
  }

  @Test
  fun disposingTheClientClosesTheWebSocket(): Unit = runBlocking {
    SampleServer().use { sampleServer ->
      ApolloClient.Builder()
          .httpServerUrl(sampleServer.graphqlUrl())
          .webSocketServerUrl(sampleServer.subscriptionsUrl())
          .build().use { apolloClient ->
            apolloClient.subscription(CountSubscription(2, 0))
                .toFlow()
                .test {
                  awaitItem()
                  awaitItem()
                  awaitComplete()
                }
          }

      ApolloClient.Builder()
          .httpServerUrl(sampleServer.graphqlUrl())
          .webSocketServerUrl(sampleServer.subscriptionsUrl())
          .build()
          .use { apolloClient ->
            delay(1000)
            // Check that the server saw the connection close
            assertEquals("Closed 0 session(s)", apolloClient.mutation(CloseSocketMutation()).execute().data?.closeAllWebSockets)
          }
    }
  }

  @Test
  fun flowThrowsIfNoReconnect() = runBlocking {
    SampleServer().use { sampleServer ->
      ApolloClient.Builder()
          .httpServerUrl(sampleServer.graphqlUrl())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(sampleServer.subscriptionsUrl())
                  .wsProtocol(SubscriptionWsProtocol {
                    mapOf("return" to "success")
                  })
                  .build()
          )
          .build()
          .use { apolloClient ->
            launch {
              delay(200)
              apolloClient.mutation(CloseSocketMutation()).execute()
            }

            apolloClient.subscription(CountSubscription(2, 500))
                .toFlow()
                .test {
                  awaitItem()
                  awaitItem().exception.apply {
                    assertIs<ApolloWebSocketClosedException>(this)
                    assertEquals(1011, code)
                  }

                  cancelAndIgnoreRemainingEvents()
                }
          }
    }
  }

  @Test
  fun closeConnectionReconnectsTheWebSocket() = runBlocking {
    SampleServer().use { sampleServer ->
      var connectionInitCount = 0
      ApolloClient.Builder()
          .httpServerUrl(sampleServer.graphqlUrl())
          .subscriptionNetworkTransport(
              WebSocketNetworkTransport.Builder()
                  .serverUrl(sampleServer.subscriptionsUrl())
                  .wsProtocol(SubscriptionWsProtocol {
                    connectionInitCount++
                    mapOf("return" to "success")
                  })
                  .build()
          )
          .addRetryOnErrorInterceptor { e, _ ->
            assertIs<DefaultApolloException>(e)
            assertEquals("oh no!", e.message)
            true
          }
          .build()
          .use { apolloClient ->
            apolloClient.subscription(CountSubscription(2, 100))
                .toFlow()
                .test {
                  awaitItem() // 0

                  apolloClient.subscriptionNetworkTransport.closeConnection(DefaultApolloException("oh no!"))

                  awaitItem() // 0 again since we've re-subscribed
                  awaitItem() // 1
                  awaitComplete()
                }

          }

      // connectionInitCount is 2 since we returned true in webSocketReopenWhen
      assertEquals(2, connectionInitCount)
    }
  }
}
