package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.CustomScalarAdapters
import com.google.common.truth.Truth.assertThat
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class WebSocketSubscriptionTransportMessageTest {
  private lateinit var webSocketFactory: MockWebSocketFactory
  private lateinit var subscriptionTransport: WebSocketSubscriptionTransport
  private lateinit var transportCallback: MockSubscriptionTransportCallback

  @Before
  fun setUp() {
    webSocketFactory = MockWebSocketFactory()
    transportCallback = MockSubscriptionTransportCallback()
    val factory = WebSocketSubscriptionTransport.Factory("wss://localhost/", webSocketFactory)
    subscriptionTransport = factory.create(transportCallback) as WebSocketSubscriptionTransport
    subscriptionTransport.connect()
    assertThat(webSocketFactory.webSocket).isNotNull()
  }

  @Test
  fun connectionInit() {
    subscriptionTransport.send(OperationClientMessage.Init(emptyMap<String, Any>()))
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("""{"type":"connection_init"}""")
    subscriptionTransport.send(OperationClientMessage.Init(mapOf("param1" to true, "param2" to "value")))
    assertThat(webSocketFactory.webSocket.lastSentMessage)
        .isEqualTo("""{"type":"connection_init","payload":{"param1":true,"param2":"value"}}""")
  }

  @Test
  fun startSubscriptionAutoPersistSubscriptionDisabled() {
    subscriptionTransport.send(OperationClientMessage.Start(subscriptionId = "subscriptionId",
        subscription = MockSubscription(),
        customScalarAdapters = CustomScalarAdapters(emptyMap()),
        autoPersistSubscription = false,
        sendSubscriptionDocument = false))
    val expected = """{"id":"subscriptionId","type":"start","payload":{"variables":{},"operationName":"SomeSubscription","query":"subscription{commentAdded{id  name}"}}"""
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo(expected)
  }

  @Test
  fun startSubscriptionAutoPersistSubscriptionEnabledSendDocumentEnabled() {
    subscriptionTransport.send(OperationClientMessage.Start("subscriptionId", MockSubscription(),
        CustomScalarAdapters(emptyMap()), autoPersistSubscription = true, sendSubscriptionDocument = true))
    val expected = """{"id":"subscriptionId","type":"start","payload":{"variables":{},"operationName":"SomeSubscription","query":"subscription{commentAdded{id  name}","extensions":{"persistedQuery":{"version":1,"sha256Hash":"someId"}}}}"""
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo(expected)
  }

  @Test
  fun startSubscriptionAutoPersistSubscriptionEnabledSendDocumentDisabled() {
    subscriptionTransport.send(OperationClientMessage.Start("subscriptionId", MockSubscription(),
        CustomScalarAdapters(emptyMap()), autoPersistSubscription = true, sendSubscriptionDocument = false))
    val expected = """{"id":"subscriptionId","type":"start","payload":{"variables":{},"operationName":"SomeSubscription","extensions":{"persistedQuery":{"version":1,"sha256Hash":"someId"}}}}"""
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo(expected)
  }

  @Test
  fun stopSubscription() {
    subscriptionTransport.send(OperationClientMessage.Stop("subscriptionId"))
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("""{"id":"subscriptionId","type":"stop"}""")
  }

  @Test
  fun terminateSubscription() {
    subscriptionTransport.send(OperationClientMessage.Terminate())
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("""{"type":"connection_terminate"}""")
  }

  @Test
  fun connectionAcknowledge() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, """{"type":"connection_ack"}""")
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.ConnectionAcknowledge::class.java)
  }

  @Test
  fun data() {
    webSocketFactory.webSocket.listener.onMessage(
        webSocketFactory.webSocket,
        """{"type":"data","id":"subscriptionId","payload":{"data":{"commentAdded":{"__typename":"Comment","id":10,"content":"test10"}}}}"""
    )
    assertThat(transportCallback.lastMessage).isEqualTo(OperationServerMessage.Data(
        id = "subscriptionId",
        payload = mapOf(
            "data" to mapOf(
                "commentAdded" to mapOf(
                    "__typename" to "Comment",
                    "id" to BigDecimal.valueOf(10),
                    "content" to "test10"
                )
            )
        )
    ))
  }

  @Test
  fun connectionError() {
    webSocketFactory.webSocket.listener.onMessage(
        webSocketFactory.webSocket,
        """{"type":"connection_error","payload":{"message":"Connection Error"}}"""
    )
    assertThat(transportCallback.lastMessage)
        .isEqualTo(OperationServerMessage.ConnectionError(mapOf("message" to "Connection Error")))
  }

  @Test
  fun error() {
    webSocketFactory.webSocket.listener.onMessage(
        webSocketFactory.webSocket,
        """{"type":"error", "id":"subscriptionId", "payload":{"message":"Error"}}"""
    )
    assertThat(transportCallback.lastMessage).isEqualTo(OperationServerMessage.Error(
        id = "subscriptionId",
        payload = mapOf("message" to "Error")
    ))
  }

  @Test
  fun complete() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, """{"type":"complete", "id":"subscriptionId"}""")
    assertThat(transportCallback.lastMessage).isEqualTo(OperationServerMessage.Complete("subscriptionId"))
  }

  @Test
  fun unsupported() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, """{"type":"unsupported"}""")
    assertThat(transportCallback.lastMessage).isEqualTo(OperationServerMessage.Unsupported("""{"type":"unsupported"}"""))
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, """{"type":"unsupported""")
    assertThat(transportCallback.lastMessage).isEqualTo(OperationServerMessage.Unsupported("""{"type":"unsupported"""))
  }

  private class MockWebSocketFactory : WebSocket.Factory {
    lateinit var webSocket: MockWebSocket

    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
      check(!::webSocket.isInitialized) { "already initialized" }
      return MockWebSocket(request, listener).also { webSocket = it }
    }
  }

  private class MockWebSocket(val request: Request, val listener: WebSocketListener) : WebSocket {
    var lastSentMessage: String? = null

    init {
      listener.onOpen(this, Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_0)
          .code(200)
          .message("Ok")
          .build()
      )
    }

    override fun request(): Request = request

    override fun queueSize(): Long = throw UnsupportedOperationException()

    override fun send(text: String): Boolean {
      lastSentMessage = text
      return true
    }

    override fun send(bytes: ByteString): Boolean = throw UnsupportedOperationException()

    override fun close(code: Int, reason: String?): Boolean = throw UnsupportedOperationException()

    override fun cancel() {
      throw UnsupportedOperationException()
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
}