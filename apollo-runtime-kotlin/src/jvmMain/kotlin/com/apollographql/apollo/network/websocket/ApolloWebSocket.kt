package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.ApolloWebSocketException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.internal.commonAsUtf8ToByteArray
import java.util.concurrent.atomic.AtomicReference

@ExperimentalCoroutinesApi
actual class ApolloWebSocketFactory(
    private val request: Request,
    private val webSocketFactory: WebSocket.Factory
) {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>
  ) : this(
      request = Request.Builder()
          .url(serverUrl.toHttpUrl())
          .headers(headers.toHeaders())
          .build(),
      webSocketFactory = OkHttpClient()
  )

  actual fun open(): ApolloWebSocketConnection = ApolloWebSocketConnection(
      request = request,
      webSocketFactory = webSocketFactory
  )
}

@ExperimentalCoroutinesApi
actual class ApolloWebSocketConnection(
    request: Request,
    webSocketFactory: WebSocket.Factory,
    private val eventChannel: Channel<Event> = Channel()
) : ReceiveChannel<ApolloWebSocketConnection.Event> by eventChannel {

  private val state = AtomicReference<State>(State.Connecting)

  init {
    webSocketFactory.newWebSocket(request = request, listener = object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (state.compareAndSet(State.Connecting, State.Connected(webSocket))) {
          eventChannel.offer(Event.Open(webSocket))
        } else {
          webSocket.cancel()
        }
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        if (state.get() is State.Connected) {
          eventChannel.offer(
              Event.Message(
                  data = text.commonAsUtf8ToByteArray().toByteString(),
                  webSocket = webSocket
              )
          )
        } else {
          webSocket.cancel()
        }
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        if (state.get() is State.Connected) {
          eventChannel.offer(
              Event.Message(
                  data = bytes,
                  webSocket = webSocket
              )
          )
        } else {
          webSocket.cancel()
        }
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        val currentState = state.get()
        if (currentState is State.Connected && state.compareAndSet(currentState, State.Disconnected)) {
          eventChannel.close(
              ApolloWebSocketException(
                  message = "Web socket communication error",
                  cause = t
              )
          )
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        val currentState = state.get()
        if (currentState is State.Connected && state.compareAndSet(currentState, State.Disconnected)) {
          eventChannel.close()
        }
      }
    })
  }

  actual fun send(data: ByteString) {
    val currentState = state.get()
    if (currentState is State.Connected) {
      currentState.webSocket.send(data)
    }
  }

  actual fun close(code: Int, reason: String?) {
    val connectedState = state.get()
    if (connectedState is State.Connected && state.compareAndSet(connectedState, State.Disconnected)) {
      connectedState.webSocket.close(code = code, reason = reason)
      eventChannel.close()
    }
  }

  actual sealed class Event {

    actual class Open(val webSocket: WebSocket) : Event()

    actual class Message(actual val data: ByteString, val webSocket: WebSocket) : Event()
  }

  sealed class State {
    object Connecting : State()

    class Connected(val webSocket: WebSocket) : State()

    object Disconnected : State()
  }
}
