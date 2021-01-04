package com.apollographql.apollo.network.ws

import com.apollographql.apollo.ApolloWebSocketException
import com.apollographql.apollo.ApolloWebSocketServerException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.dispatcher.ApolloCoroutineDispatcherContext
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.testing.MockSubscription
import com.apollographql.apollo.testing.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.toList
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ApolloExperimental
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class ApolloWebSocketNetworkTransportTest {

  @Test
  fun `when multiple responses, assert all delivered and completed`() {
    runBlocking {
      val expectedRequest = ApolloRequest(
          operation = MockSubscription(),
          customScalarAdapters = CustomScalarAdapters.DEFAULT,
          executionContext = ApolloCoroutineDispatcherContext(Dispatchers.Unconfined)
      )
      val expectedResponses = listOf(
          "{\"data\":{\"name\":\"MockQuery1\"}}",
          "{\"data\":{\"name\":\"MockQuery2\"}}",
          "{\"data\":{\"name\":\"MockQuery3\"}}"
      )
      val webSocketConnection = WebSocketConnectionMock(
          expectedRequest = expectedRequest,
          expectedOnStartResponse = expectedResponses.first()
      )

      ApolloWebSocketNetworkTransport(
          webSocketFactory = object : WebSocketFactory {
            override suspend fun open(headers: Map<String, String>): WebSocketConnection = webSocketConnection
          },
          idleTimeoutMs = -1
      ).execute(
          request = expectedRequest,
          executionContext = ExecutionContext.Empty
      ).collectIndexed { index, actualResponse ->
        assertEquals(expectedResponses[index], actualResponse.response.data?.rawResponse)
        if (index < expectedResponses.size - 1) {
          webSocketConnection.enqueueResponse(expectedResponses[index + 1])
        } else {
          webSocketConnection.enqueueComplete()
        }
      }
    }
  }

  @Test
  fun `when connection ack timeout, assert completed with exception`() {
    runBlocking {
      val expectedRequest = ApolloRequest(
          operation = MockSubscription(),
          customScalarAdapters = CustomScalarAdapters.DEFAULT,
          executionContext = ApolloCoroutineDispatcherContext(Dispatchers.Unconfined)
      )

      val result = runCatching {
        ApolloWebSocketNetworkTransport(
            webSocketFactory = object : WebSocketFactory {
              override suspend fun open(headers: Map<String, String>): WebSocketConnection = FrozenWebSocketConnection()
            },
            connectionAcknowledgeTimeoutMs = 1_000,
            idleTimeoutMs = -1
        ).execute(
            request = expectedRequest,
            executionContext = ApolloCoroutineDispatcherContext(Dispatchers.Unconfined)
        ).toList()
      }

      assertTrue(result.isFailure)
      assertTrue(result.exceptionOrNull() is ApolloWebSocketException)
    }
  }

  @Test
  fun `when subscription error, assert completed with exception and payload`() {
    val result = runBlocking {
      val expectedRequest = ApolloRequest(
          operation = MockSubscription(),
          customScalarAdapters = CustomScalarAdapters.DEFAULT,
          executionContext = ApolloCoroutineDispatcherContext(Dispatchers.Unconfined)
      )
      val expectedOnStartResponse = "{\"data\":{\"name\":\"MockQuery\"}}"
      val webSocketConnection = WebSocketConnectionMock(
          expectedRequest = expectedRequest,
          expectedOnStartResponse = expectedOnStartResponse
      )

      runCatching {
        ApolloWebSocketNetworkTransport(
            webSocketFactory = object : WebSocketFactory {
              override suspend fun open(headers: Map<String, String>): WebSocketConnection = webSocketConnection
            },
            idleTimeoutMs = -1
        ).execute(
            request = expectedRequest,
            executionContext = ExecutionContext.Empty
        ).collect { actualResponse ->
          assertEquals(expectedOnStartResponse, actualResponse.response.data?.rawResponse)
          webSocketConnection.enqueueError("{\"key1\":\"value1\", \"key2\":\"value2\"}")
        }
      }
    }

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is ApolloWebSocketServerException)
    assertEquals("value1", (result.exceptionOrNull() as ApolloWebSocketServerException).payload["key1"])
    assertEquals("value2", (result.exceptionOrNull() as ApolloWebSocketServerException).payload["key2"])
  }

  @Test
  fun `when no active subscriptions, assert web socket connection closed`() {
    runBlocking {
      val expectedRequest = ApolloRequest(
          operation = MockSubscription(),
          customScalarAdapters = CustomScalarAdapters.DEFAULT,
          executionContext = ApolloCoroutineDispatcherContext(Dispatchers.Unconfined)
      )
      val expectedOnStartResponse = "{\"data\":{\"name\":\"MockQuery\"}}"
      val webSocketConnection = WebSocketConnectionMock(
          expectedRequest = expectedRequest,
          expectedOnStartResponse = expectedOnStartResponse
      )

      ApolloWebSocketNetworkTransport(
          webSocketFactory = object : WebSocketFactory {
            override suspend fun open(headers: Map<String, String>): WebSocketConnection = webSocketConnection
          },
          idleTimeoutMs = 1_000
      ).execute(
          request = expectedRequest,
          executionContext = ExecutionContext.Empty
      ).collect { actualResponse ->
        assertEquals(expectedOnStartResponse, actualResponse.response.data?.rawResponse)
        webSocketConnection.enqueueComplete()
      }

      webSocketConnection.isClosed.await()
    }
  }

  @Test
  fun `when connection keep alive timeout, assert web socket connection closed`() {
    runBlocking {
      val expectedRequest = ApolloRequest(
          operation = MockSubscription(),
          customScalarAdapters = CustomScalarAdapters.DEFAULT,
          executionContext = ApolloCoroutineDispatcherContext(Dispatchers.Unconfined)
      )
      val expectedOnStartResponse = "{\"data\":{\"name\":\"MockQuery\"}}"
      val webSocketConnection = WebSocketConnectionMock(
          expectedRequest = expectedRequest,
          expectedOnStartResponse = expectedOnStartResponse
      )

      ApolloWebSocketNetworkTransport(
          webSocketFactory = object : WebSocketFactory {
            override suspend fun open(headers: Map<String, String>): WebSocketConnection = webSocketConnection
          },
          idleTimeoutMs = -1,
          connectionKeepAliveTimeoutMs = 1_000
      ).execute(
          request = expectedRequest,
          executionContext = ExecutionContext.Empty
      ).collect { actualResponse ->
        assertEquals(expectedOnStartResponse, actualResponse.response.data?.rawResponse)
        webSocketConnection.enqueueConnectionKeepAlive()
      }

      webSocketConnection.isClosed.await()
    }
  }
}

private class FrozenWebSocketConnection(
    private val receivedMessageChannel: Channel<ByteString> = Channel()
) : WebSocketConnection, ReceiveChannel<ByteString> by receivedMessageChannel {
  override fun send(data: ByteString) {
  }

  override fun close() {
  }
}

private class WebSocketConnectionMock(
    val expectedRequest: ApolloRequest<MockSubscription.Data>,
    val expectedOnStartResponse: String,
    private val receivedMessageChannel: Channel<ByteString> = Channel(Channel.BUFFERED)
) : WebSocketConnection, ReceiveChannel<ByteString> by receivedMessageChannel {
  val isClosed = CompletableDeferred<Boolean>()

  override fun send(data: ByteString) {
    when (data.utf8()) {
      ApolloGraphQLClientMessage.Init(emptyMap()).serialize().utf8() -> {
        receivedMessageChannel.offer("{\"type\": \"connection_ack\"}".encodeUtf8())
      }

      ApolloGraphQLClientMessage.Start(expectedRequest).serialize().utf8() -> {
        enqueueResponse(expectedOnStartResponse)
      }
    }
  }

  override fun close() {
    isClosed.complete(true)
    receivedMessageChannel.close()
  }

  fun enqueueResponse(payload: String) {
    receivedMessageChannel.offer(
        "{\"type\":\"data\", \"id\":\"${expectedRequest.requestUuid}\", \"payload\":$payload}".encodeUtf8()
    )
  }

  fun enqueueError(payload: String) {
    receivedMessageChannel.offer(
        "{\"type\":\"error\", \"id\":\"${expectedRequest.requestUuid}\", \"payload\":$payload}".encodeUtf8()
    )
  }

  fun enqueueConnectionKeepAlive() {
    receivedMessageChannel.offer(
        "{\"type\":\"ka\"}".encodeUtf8()
    )
  }

  fun enqueueComplete() {
    receivedMessageChannel.offer(
        "{\"type\": \"complete\", \"id\":\"${expectedRequest.requestUuid}\"}".encodeUtf8()
    )
  }
}
