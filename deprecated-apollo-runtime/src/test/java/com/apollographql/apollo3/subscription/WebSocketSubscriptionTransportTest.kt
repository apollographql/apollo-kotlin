package com.apollographql.apollo3.subscription

import com.google.common.truth.Truth.assertThat
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WebSocketSubscriptionTransportTest {
  private lateinit var webSocketRequest: Request
  private lateinit var webSocketFactory: MockWebSocketFactory
  private lateinit var subscriptionTransport: WebSocketSubscriptionTransport
  
  @Before
  @Throws(Exception::class)
  fun setUp() {
    webSocketRequest = Request.Builder().url("wss://localhost").build()
    webSocketFactory = MockWebSocketFactory()
    val factory = WebSocketSubscriptionTransport.Factory("wss://localhost/", webSocketFactory)
    subscriptionTransport = factory.create(object : SubscriptionTransport.Callback {
      override fun onConnected() {}
      override fun onFailure(t: Throwable) {}
      override fun onMessage(message: OperationServerMessage) {}
      override fun onClosed() {}
    }) as WebSocketSubscriptionTransport
  }

  @Test
  fun connect() {
    assertThat(subscriptionTransport.webSocket.get()).isNull()
    assertThat(subscriptionTransport.webSocketListener.get()).isNull()
    subscriptionTransport.connect()
    assertThat(subscriptionTransport.webSocket.get()).isNotNull()
    assertThat(subscriptionTransport.webSocketListener.get()).isNotNull()
    assertThat(webSocketFactory.request.header("Sec-WebSocket-Protocol")).isEqualTo("graphql-ws")
    assertThat(webSocketFactory.request.header("Cookie")).isEqualTo("")
  }

  @Test
  fun disconnect() {
    subscriptionTransport.connect()
    assertThat(subscriptionTransport.webSocket.get()).isNotNull()
    assertThat(subscriptionTransport.webSocketListener.get()).isNotNull()
    subscriptionTransport.disconnect(OperationClientMessage.Terminate())
    assertThat(subscriptionTransport.webSocket.get()).isNull()
    assertThat(subscriptionTransport.webSocketListener.get()).isNull()
  }

  @Test
  fun send() {
    val callbackFailure = AtomicReference<Throwable?>()
    subscriptionTransport = WebSocketSubscriptionTransport(webSocketRequest, webSocketFactory, object : SubscriptionTransport.Callback {
      override fun onConnected() {}
      override fun onFailure(t: Throwable) {
        callbackFailure.set(t)
      }

      override fun onMessage(message: OperationServerMessage) {}
      override fun onClosed() {}
    })
    subscriptionTransport.send(OperationClientMessage.Init(emptyMap<String, Any>()))
    assertThat(callbackFailure.get()).isInstanceOf(IllegalStateException::class.java)
    callbackFailure.set(null)
    subscriptionTransport.connect()
    subscriptionTransport.send(OperationClientMessage.Init(emptyMap<String, Any>()))
    assertThat(callbackFailure.get()).isNull()
    subscriptionTransport.disconnect(OperationClientMessage.Terminate())
    subscriptionTransport.send(OperationClientMessage.Init(emptyMap<String, Any>()))
    assertThat(callbackFailure.get()).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun subscriptionTransportCallback() {
    val callbackConnected = AtomicBoolean()
    val callbackFailure = AtomicReference<Throwable>()
    val callbackMessage = AtomicReference<OperationServerMessage>()
    subscriptionTransport = WebSocketSubscriptionTransport(webSocketRequest, webSocketFactory, object : SubscriptionTransport.Callback {
      override fun onConnected() {
        callbackConnected.set(true)
      }

      override fun onFailure(t: Throwable) {
        callbackFailure.set(t)
      }

      override fun onMessage(message: OperationServerMessage) {
        callbackMessage.set(message)
      }

      override fun onClosed() {}
    })
    subscriptionTransport.connect()
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"connection_ack\"}")
    webSocketFactory.webSocket.listener.onFailure(webSocketFactory.webSocket, UnsupportedOperationException(), null)
    assertThat(callbackConnected.get()).isTrue()
    assertThat(callbackMessage.get()).isInstanceOf(OperationServerMessage.ConnectionAcknowledge::class.java)
    assertThat(callbackFailure.get()).isInstanceOf(UnsupportedOperationException::class.java)
  }

  @Test
  fun subscriptionTransportClosedCallback() {
    val callbackConnected = AtomicBoolean()
    val callbackClosed = AtomicBoolean()
    subscriptionTransport = WebSocketSubscriptionTransport(webSocketRequest, webSocketFactory, object : SubscriptionTransport.Callback {
      override fun onConnected() {
        callbackConnected.set(true)
      }

      override fun onFailure(t: Throwable) {
        throw UnsupportedOperationException("Unexpected")
      }

      override fun onMessage(message: OperationServerMessage) {}
      override fun onClosed() {
        callbackClosed.set(true)
      }
    })
    subscriptionTransport.connect()
    webSocketFactory.webSocket.listener.onClosed(webSocketFactory.webSocket, 1001, "")
    assertThat(callbackConnected.get()).isTrue()
    assertThat(callbackClosed.get()).isTrue()
  }

  private class MockWebSocketFactory : WebSocket.Factory {
    lateinit var request: Request
    lateinit var webSocket: MockWebSocket

    override fun newWebSocket(request: Request, listener: WebSocketListener): WebSocket {
      this.request = request
      return MockWebSocket(request, listener).also { webSocket = it }
    }
  }

  private class MockWebSocket(val request: Request, val listener: WebSocketListener) : WebSocket {
    var lastSentMessage: String? = null
    var closed = false

    override fun request(): Request {
      return request
    }

    override fun queueSize(): Long = throw UnsupportedOperationException()

    override fun send(text: String): Boolean {
      lastSentMessage = text
      return true
    }

    override fun send(bytes: ByteString): Boolean {
      throw UnsupportedOperationException()
    }

    override fun close(code: Int, reason: String?): Boolean {
      closed = true
      return true
    }

    override fun cancel() {
      throw UnsupportedOperationException()
    }

    init {
      listener.onOpen(this, Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_0)
          .code(200)
          .message("Ok")
          .build()
      )
    }
  }
}