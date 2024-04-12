package com.apollographql.apollo3.testing

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.WebSocketBody
import com.apollographql.apollo3.mockserver.WebsocketMockRequest
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.network.websocket.WebSocketNetworkTransport
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.CoroutineScope
import okio.use

@ApolloExperimental
class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

/**
 * A convenience function that makes sure the MockServer and ApolloClient are properly closed at the end of the test
 */
@ApolloExperimental
fun mockServerTest(
    skipDelays: Boolean = true,
    clientBuilder: ApolloClient.Builder.() -> Unit = {},
    block: suspend MockServerTest.() -> Unit
) = runTest(skipDelays) {
  val mockServer = MockServer()

  val apolloClient = ApolloClient.Builder()
      .serverUrl(mockServer.url())
      .apply(clientBuilder)
      .build()

  try {
    apolloClient.use {
      MockServerTest(mockServer, it, this).block()
    }
  } finally {
    mockServer.close()
  }
}

@ApolloExperimental
class MockServerWebSocketTest(
    val apolloClient: ApolloClient,
    private val mockServer: MockServer,
    val coroutineScope: CoroutineScope,
) {
  /**
   * Enqueue the response straight away
   */
  val serverWriter: WebSocketBody = mockServer.enqueueWebSocket()
  private var _serverReader: WebsocketMockRequest? = null

  val serverReader: WebsocketMockRequest
    get() {
      check(_serverReader != null) {
        "You need to call awaitConnectionInit or awaitWebSocketRequest first"
      }
      return _serverReader!!
    }

  suspend fun awaitWebSocketRequest() {
    _serverReader = mockServer.awaitWebSocketRequest()
  }

  suspend fun awaitConnectionInit() {
    awaitWebSocketRequest()

    serverReader.awaitMessage()
    serverWriter.enqueueMessage(connectionAckMessage())
  }
}

@ApolloExperimental
fun mockServerWebSocketTest(customizeTransport: WebSocketNetworkTransport.Builder.() -> Unit = {}, block: suspend MockServerWebSocketTest.() -> Unit) = runTest(false) {
  MockServer().use { mockServer ->

    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .subscriptionNetworkTransport(
            WebSocketNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .apply(customizeTransport)
                .build()
        )
        .build().use { apolloClient ->
          MockServerWebSocketTest(apolloClient, mockServer, this@runTest).block()
        }
  }
}