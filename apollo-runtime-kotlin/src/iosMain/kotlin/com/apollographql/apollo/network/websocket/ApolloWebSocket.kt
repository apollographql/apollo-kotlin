package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.ApolloWebSocketException
import com.apollographql.apollo.network.toNSData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.IOException
import okio.internal.commonAsUtf8ToByteArray
import okio.toByteString
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketMessageTypeData
import platform.Foundation.NSURLSessionWebSocketMessageTypeString
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.setValue
import kotlin.native.concurrent.AtomicReference

typealias WebSocketFactory = (NSURLRequest) -> NSURLSessionWebSocketTask

actual class ApolloWebSocketFactory(
    private val request: NSURLRequest,
    private val webSocketFactory: WebSocketFactory
) {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>
  ) : this(
      request = NSMutableURLRequest.requestWithURL(NSURL(string = serverUrl)).apply {
        headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      },
      webSocketFactory = {
        NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
            delegate = null,
            delegateQueue = NSOperationQueue.mainQueue
        ).webSocketTaskWithRequest(it)
      }
  )

  actual fun open(): ApolloWebSocketConnection = ApolloWebSocketConnection(
      request = request,
      webSocketFactory = webSocketFactory
  )
}

actual class ApolloWebSocketConnection(
    request: NSURLRequest,
    webSocketFactory: WebSocketFactory,
    private val eventChannel: Channel<Event> = Channel()
) : ReceiveChannel<ApolloWebSocketConnection.Event> by eventChannel {
  private val state = AtomicReference<State>(State.Connecting)

  init {
    val webSocket = webSocketFactory(request).apply { resume() }
    state.value = State.Connected(webSocket)
    webSocket.receiveNext()
  }

  actual fun send(data: ByteString) {
    assert(NSThread.isMainThread())

    val currentState = state.value
    if (currentState is State.Connected) {
      val message = NSURLSessionWebSocketMessage(data.toByteArray().toNSData())
      currentState.webSocket.sendMessage(message) { error ->
        if (error != null) {
          state.value = State.Disconnected
          eventChannel.close(
              ApolloWebSocketException(
                  message = "Web socket communication error",
                  cause = IOException(error.localizedDescription)
              )
          )
        }
      }
    }
  }

  actual fun close(code: Int, reason: String?) {
    assert(NSThread.isMainThread())

    val connectedState = state.value
    if (connectedState is State.Connected && state.compareAndSet(connectedState, State.Disconnected)) {
      connectedState.webSocket.cancelWithCloseCode(
          code as NSURLSessionWebSocketCloseCode,
          reason?.commonAsUtf8ToByteArray()?.toNSData()
      )
    }
  }

  private fun NSURLSessionWebSocketTask.receiveNext() {
    receiveMessageWithCompletionHandler { message, error ->
      if (error == null) {
        val event = when (message?.type) {
          NSURLSessionWebSocketMessageTypeData -> {
            message.data
                ?.toByteString()
                ?.let { data ->
                  Event.Message(
                      data = data,
                      webSocketTask = this
                  )
                }
          }

          NSURLSessionWebSocketMessageTypeString -> {
            message.string
                ?.commonAsUtf8ToByteArray()
                ?.toByteString()
                ?.let { data ->
                  Event.Message(
                      data = data,
                      webSocketTask = this
                  )
                }
          }

          else -> null
        }

        if (event != null) eventChannel.offer(event)

        receiveNext()
      } else {
        eventChannel.close(
            ApolloWebSocketException(
                message = "Web socket communication error",
                cause = IOException(error.localizedDescription)
            )
        )
        cancel()
      }
    }
  }

  actual sealed class Event {

    actual class Open(val webSocketTask: NSURLSessionWebSocketTask) : Event()

    actual class Message(actual val data: ByteString, val webSocketTask: NSURLSessionWebSocketTask) : Event()
  }

  sealed class State {
    object Connecting : State()

    class Connected(val webSocket: NSURLSessionWebSocketTask) : State()

    object Disconnected : State()
  }
}
