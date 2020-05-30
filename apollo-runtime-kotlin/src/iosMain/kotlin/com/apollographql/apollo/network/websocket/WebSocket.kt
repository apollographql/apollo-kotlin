package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.ApolloWebSocketException
import com.apollographql.apollo.network.toNSData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.IOException
import okio.internal.commonAsUtf8ToByteArray
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketMessageTypeData
import platform.Foundation.NSURLSessionWebSocketMessageTypeString
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.setValue
import platform.darwin.NSObject

interface WebSocketConnectionListener {
  fun onOpen(webSocket: NSURLSessionWebSocketTask)

  fun onClose(webSocket: NSURLSessionWebSocketTask, code: NSURLSessionWebSocketCloseCode)
}

typealias NSWebSocketFactory = (NSURLRequest, WebSocketConnectionListener) -> NSURLSessionWebSocketTask

@ExperimentalCoroutinesApi
actual class WebSocketFactory(
    private val request: NSURLRequest,
    private val webSocketFactory: NSWebSocketFactory
) {

  actual constructor(
      serverUrl: String,
      headers: Map<String, String>
  ) : this(
      request = NSMutableURLRequest.requestWithURL(NSURL(string = serverUrl)).apply {
        headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
      },
      webSocketFactory = { request, connectionListener ->
        NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
            delegate = NSURLSessionWebSocketDelegate(connectionListener),
            delegateQueue = NSOperationQueue.mainQueue
        ).webSocketTaskWithRequest(request)
      }
  )

  actual suspend fun open(): WebSocketConnection {
    assert(NSThread.isMainThread())

    val messageChannel = Channel<ByteString>(Channel.BUFFERED)
    val webSocketConnectionDeferred = CompletableDeferred<NSURLSessionWebSocketTask>()

    val connectionListener = object : WebSocketConnectionListener {
      override fun onOpen(webSocket: NSURLSessionWebSocketTask) {
        if (!webSocketConnectionDeferred.complete(webSocket)) {
          webSocket.cancel()
        }
      }

      override fun onClose(webSocket: NSURLSessionWebSocketTask, code: NSURLSessionWebSocketCloseCode) {
        webSocketConnectionDeferred.cancel()
        messageChannel.close()
      }
    }

    val webSocket = webSocketFactory(request, connectionListener)
        .apply { resume() }

    try {
      return WebSocketConnection(
          webSocket = webSocketConnectionDeferred.await(),
          messageChannel = messageChannel
      )
    } finally {
      webSocket.cancel()
    }
  }
}

@ExperimentalCoroutinesApi
actual class WebSocketConnection(
    private val webSocket: NSURLSessionWebSocketTask,
    private val messageChannel: Channel<ByteString>
) : ReceiveChannel<ByteString> by messageChannel {

  init {
    messageChannel.invokeOnClose {
      webSocket.cancelWithCloseCode(
          closeCode = 1001,
          reason = null
      )
    }
    webSocket.receiveNext()
  }

  actual fun send(data: ByteString) {
    assert(NSThread.isMainThread())

    if (!messageChannel.isClosedForReceive) {
      val message = NSURLSessionWebSocketMessage(data.toByteArray().toNSData())
      webSocket.sendMessage(message) { error ->
        if (error != null) {
          messageChannel.close(
              ApolloWebSocketException(
                  message = "Web socket communication error",
                  cause = IOException(error.localizedDescription)
              )
          )
        }
      }
    }
  }

  actual fun close() {
    assert(NSThread.isMainThread())
    messageChannel.close()
  }

  private fun NSURLSessionWebSocketTask.receiveNext() {
    receiveMessageWithCompletionHandler { message, error ->
      if (error == null) {
        val data = when (message?.type) {
          NSURLSessionWebSocketMessageTypeData -> {
            message.data?.toByteString()
          }

          NSURLSessionWebSocketMessageTypeString -> {
            message.string?.commonAsUtf8ToByteArray()?.toByteString()
          }

          else -> null
        }

        try {
          if (data != null) messageChannel.offer(data)
        } catch (e: Exception) {
          cancel()
          throw e
        }

        receiveNext()
      } else {
        messageChannel.close(
            ApolloWebSocketException(
                message = "Web socket communication error",
                cause = IOException(error.localizedDescription)
            )
        )
        cancel()
      }
    }
  }
}

private class NSURLSessionWebSocketDelegate(
    val webSocketConnectionListener: WebSocketConnectionListener
) : NSObject(), NSURLSessionWebSocketDelegateProtocol {

  override fun URLSession(
      session: NSURLSession,
      webSocketTask: NSURLSessionWebSocketTask,
      didOpenWithProtocol: String?
  ) {
    webSocketConnectionListener.onOpen(webSocketTask)
  }

  override fun URLSession(
      session: NSURLSession,
      webSocketTask: NSURLSessionWebSocketTask,
      didCloseWithCode: NSURLSessionWebSocketCloseCode,
      reason: NSData?
  ) {
    webSocketConnectionListener.onClose(webSocket = webSocketTask, code = didCloseWithCode)
  }
}
