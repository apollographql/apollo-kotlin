package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.CustomTypeAdapter
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.google.common.truth.Truth.assertThat
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.BufferedSource
import okio.ByteString
import org.junit.Test
import java.math.BigDecimal

class WebSocketSubscriptionTransportMessageTest {
  private val webSocketFactory = MockWebSocketFactory()
  private val subscriptionTransport: WebSocketSubscriptionTransport
  private val transportCallback = MockSubscriptionTransportCallback()

  init {
    val factory = WebSocketSubscriptionTransport.Factory("wss://localhost/", webSocketFactory)
    subscriptionTransport = factory.create(transportCallback) as WebSocketSubscriptionTransport
    subscriptionTransport.connect()
    assertThat(webSocketFactory.webSocket).isNotNull()
  }

  @Test
  fun connectionInit() {
    subscriptionTransport.send(OperationClientMessage.Init(emptyMap()))
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"type\":\"connection_init\"}")
    subscriptionTransport.send(
        OperationClientMessage.Init(
            mapOf("param1" to true, "param2" to "value")
        )
    )
    assertThat(webSocketFactory.webSocket.lastSentMessage)
        .isEqualTo("{\"type\":\"connection_init\",\"payload\":{\"param1\":true,\"param2\":\"value\"}}")
  }

  @Test
  fun startSubscriptionAutoPersistSubscriptionDisabled() {
    subscriptionTransport.send(OperationClientMessage.Start("subscriptionId", MockSubscription(),
        ScalarTypeAdapters(emptyMap<ScalarType, CustomTypeAdapter<*>>()), false, false))
    val expected = ("{\"id\":\"subscriptionId\",\"type\":\"start\",\"payload\":{\"variables\":{},"
        + "\"operationName\":\"SomeSubscription\",\"query\":\"subscription{commentAdded{id  name}\"}}")
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo(expected)
  }

  @Test
  fun startSubscriptionAutoPersistSubscriptionEnabledSendDocumentEnabled() {
    subscriptionTransport.send(OperationClientMessage.Start("subscriptionId", MockSubscription(),
        ScalarTypeAdapters(emptyMap<ScalarType, CustomTypeAdapter<*>>()), true, true))
    val expected = ("{\"id\":\"subscriptionId\",\"type\":\"start\",\"payload\":{\"variables\":{},"
        + "\"operationName\":\"SomeSubscription\",\"query\":\"subscription{commentAdded{id  name}\","
        + "\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"someId\"}}}}")
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo(expected)
  }

  @Test
  fun startSubscriptionAutoPersistSubscriptionEnabledSendDocumentDisabled() {
    subscriptionTransport.send(OperationClientMessage.Start("subscriptionId", MockSubscription(),
        ScalarTypeAdapters(emptyMap<ScalarType, CustomTypeAdapter<*>>()), true, false))
    val expected = ("{\"id\":\"subscriptionId\",\"type\":\"start\",\"payload\":{\"variables\":{},"
        + "\"operationName\":\"SomeSubscription\",\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"someId\"}}}}")
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo(expected)
  }

  @Test
  fun stopSubscription() {
    subscriptionTransport.send(OperationClientMessage.Stop("subscriptionId"))
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"id\":\"subscriptionId\",\"type\":\"stop\"}")
  }

  @Test
  fun terminateSubscription() {
    subscriptionTransport.send(OperationClientMessage.Terminate())
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"type\":\"connection_terminate\"}")
  }

  @Test
  fun connectionAcknowledge() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"connection_ack\"}")
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.ConnectionAcknowledge::class.java)
  }

  @Test
  fun data() {
    webSocketFactory.webSocket.listener.onMessage(
        webSocketFactory.webSocket, "{\"type\":\"data\",\"id\":\"subscriptionId\",\"payload\":{\"data\":{\"commentAdded\":"
        + "{\"__typename\":\"Comment\",\"id\":10,\"content\":\"test10\"}}}}")
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Data::class.java)
    assertThat((transportCallback.lastMessage as OperationServerMessage.Data).id).isEqualTo("subscriptionId")
    assertThat(((transportCallback.lastMessage as OperationServerMessage.Data).payload["data"] as Map<String?, Any?>)["commentAdded"] as Map<String?, Any?>?
    ).containsExactlyEntriesIn(
        mapOf<String, Any>(
            "__typename" to "Comment",
            "id" to BigDecimal.valueOf(10),
            "content" to "test10"
        )
    )
  }

  @Test
  fun connectionError() {
    webSocketFactory.webSocket.listener.onMessage(
        webSocketFactory.webSocket,
        "{\"type\":\"connection_error\",\"payload\":{\"message\":\"Connection Error\"}}"
    )
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.ConnectionError::class.java)
    assertThat((transportCallback.lastMessage as OperationServerMessage.ConnectionError).payload)
        .containsExactly("message", "Connection Error")
  }

  @Test
  fun error() {
    webSocketFactory.webSocket.listener.onMessage(
        webSocketFactory.webSocket,
        "{\"type\":\"error\", \"id\":\"subscriptionId\", \"payload\":{\"message\":\"Error\"}}"
    )
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Error::class.java)
    assertThat((transportCallback.lastMessage as OperationServerMessage.Error).id).isEqualTo("subscriptionId")
    assertThat((transportCallback.lastMessage as OperationServerMessage.Error).payload).containsExactly("message", "Error")
  }

  @Test
  fun complete() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"complete\", \"id\":\"subscriptionId\"}")
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Complete::class.java)
    assertThat((transportCallback.lastMessage as OperationServerMessage.Complete).id).isEqualTo("subscriptionId")
  }

  @Test
  fun unsupported() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"unsupported\"}")
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Unsupported::class.java)
    assertThat((transportCallback.lastMessage as OperationServerMessage.Unsupported).rawMessage).isEqualTo("{\"type\":\"unsupported\"}")
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"unsupported")
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Unsupported::class.java)
    assertThat((transportCallback.lastMessage as OperationServerMessage.Unsupported).rawMessage).isEqualTo("{\"type\":\"unsupported")
  }

  private class MockWebSocketFactory : WebSocket.Factory {
    lateinit var webSocket: MockWebSocket
    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
      return MockWebSocket(request, listener).also { webSocket = it }
    }
  }

  private class MockWebSocket(val request: Request, val listener: WebSocketListener) : WebSocket {
    var lastSentMessage: String? = null
    override fun request() = request


    override fun send(text: String): Boolean {
      lastSentMessage = text
      return true
    }

    override fun queueSize() = throw UnsupportedOperationException()
    override fun send(bytes: ByteString) = throw UnsupportedOperationException()
    override fun close(code: Int, reason: String?) = throw UnsupportedOperationException()
    override fun cancel() = throw UnsupportedOperationException()

    init {
      listener.onOpen(this, okhttp3.Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_0)
          .code(200)
          .message("Ok")
          .build()
      )
    }
  }

  private class MockSubscriptionTransportCallback : SubscriptionTransport.Callback {
    var lastMessage: OperationServerMessage? = null
    override fun onConnected() {}
    override fun onFailure(t: Throwable) {}
    override fun onMessage(message: OperationServerMessage) {
      lastMessage = message
    }

    override fun onClosed() {}
  }

  private class MockSubscription : Subscription<Operation.Data, Operation.Data, Operation.Variables> {
    override fun queryDocument() = "subscription{commentAdded{id  name}"

    override fun variables() = Operation.EMPTY_VARIABLES


    override fun wrapData(data: Operation.Data?) = data

    override fun name() = object : OperationName {
      override fun name() = "SomeSubscription"
    }

    override fun operationId() = "someId"

    override fun parse(source: BufferedSource) = throw UnsupportedOperationException()
    override fun responseFieldMapper() = throw UnsupportedOperationException()
    override fun parse(source: BufferedSource, scalarTypeAdapters: ScalarTypeAdapters) = throw UnsupportedOperationException()
    override fun parse(byteString: ByteString) = throw UnsupportedOperationException()
    override fun parse(byteString: ByteString, scalarTypeAdapters: ScalarTypeAdapters) = throw UnsupportedOperationException()
    override fun composeRequestBody(autoPersistQueries: Boolean, withQueryDocument: Boolean, scalarTypeAdapters: ScalarTypeAdapters) = throw UnsupportedOperationException()
    override fun composeRequestBody(scalarTypeAdapters: ScalarTypeAdapters) = throw UnsupportedOperationException()
    override fun composeRequestBody() = throw UnsupportedOperationException()
  }
}