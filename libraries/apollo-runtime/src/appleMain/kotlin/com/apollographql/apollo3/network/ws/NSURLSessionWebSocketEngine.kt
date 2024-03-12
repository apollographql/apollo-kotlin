package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_2_2
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.internal.ChannelWrapper
import com.apollographql.apollo3.network.toNSData
import kotlinx.cinterop.convert
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import okio.ByteString
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionWebSocketCloseCode
import platform.Foundation.NSURLSessionWebSocketDelegateProtocol
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketMessageTypeData
import platform.Foundation.NSURLSessionWebSocketMessageTypeString
import platform.Foundation.NSURLSessionWebSocketTask
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.NSObject

interface WebSocketConnectionListener {
  fun onOpen(webSocket: NSURLSessionWebSocketTask)

  fun onClose(webSocket: NSURLSessionWebSocketTask, code: NSURLSessionWebSocketCloseCode)

  fun onError(error: NSError?)
}

typealias NSWebSocketFactory = (NSURLRequest, WebSocketConnectionListener) -> NSURLSessionWebSocketTask

actual class DefaultWebSocketEngine(
    private val webSocketFactory: NSWebSocketFactory,
) : WebSocketEngine {

  actual constructor() : this(
      webSocketFactory = { request, connectionListener ->
        NSURLSession.sessionWithConfiguration(
            configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
            delegate = NSURLSessionWebSocketDelegate(connectionListener),
            delegateQueue = NSOperationQueue.mainQueue
        ).webSocketTaskWithRequest(request)
      }
  )

  override suspend fun open(
      url: String,
      headers: List<HttpHeader>,
  ): WebSocketConnection {

    val serverUrl = NSURL(string = url)

    val request = NSMutableURLRequest.requestWithURL(serverUrl).apply {
      headers.forEach { setValue(it.value, forHTTPHeaderField = it.name) }
      setHTTPMethod("GET")
    }

    val messageChannel = ChannelWrapper(Channel<String>(Channel.UNLIMITED))
    val isOpen = CompletableDeferred<Boolean>()

    val connectionListener = object : WebSocketConnectionListener {
      override fun onOpen(webSocket: NSURLSessionWebSocketTask) {
        if (!isOpen.complete(true)) {
          webSocket.cancel()
        }
      }

      override fun onClose(webSocket: NSURLSessionWebSocketTask, code: NSURLSessionWebSocketCloseCode) {
        isOpen.cancel()
        messageChannel.close()
      }

      override fun onError(error: NSError?) {
        if (error != null) {
          isOpen.cancel()
          messageChannel.close()
        }
      }
    }

    val webSocket = webSocketFactory(request, connectionListener)
        .apply { resume() }

    try {
      isOpen.await()
      return WebSocketConnectionImpl(
          webSocket = webSocket,
          messageChannel = messageChannel
      )
    } catch (e: Exception) {
      webSocket.cancel()
      throw e
    }
  }

  @Deprecated(
      "Use open(String, List<HttpHeader>) instead.",
      ReplaceWith(
          "open(url, headers.map { HttpHeader(it.key, it.value })",
          "com.apollographql.apollo3.api.http.HttpHeader"
      )
  )
  @ApolloDeprecatedSince(v3_2_2)
  override suspend fun open(url: String, headers: Map<String, String>): WebSocketConnection =
      open(url, headers.map { HttpHeader(it.key, it.value) })
}

private class WebSocketConnectionImpl(
    val webSocket: NSURLSessionWebSocketTask,
    val messageChannel: ChannelWrapper<String>,
) : WebSocketConnection {
  init {
    messageChannel.setInvokeOnClose {
      webSocket.cancelWithCloseCode(
          closeCode = CLOSE_NORMAL.convert(),
          reason = null
      )
    }
    receiveNext()
  }

  override suspend fun receive(): String {
    return messageChannel.receive()
  }

  override fun send(data: ByteString) {
    if (!messageChannel.isClosed) {
      val message = NSURLSessionWebSocketMessage(data.toByteArray().toNSData())
      val completionHandler = { error: NSError? ->
        if (error != null) handleError(error)
      }
      webSocket.sendMessage(message, completionHandler)
    }
  }

  override fun send(string: String) {
    if (!messageChannel.isClosed) {
      val message = NSURLSessionWebSocketMessage(string)
      val completionHandler = { error: NSError? ->
        if (error != null) handleError(error)
      }
      webSocket.sendMessage(message, completionHandler)
    }
  }

  override fun close() {
    messageChannel.close()
  }

  fun receiveNext() {
    val completionHandler = { message: NSURLSessionWebSocketMessage?, error: NSError? ->
      if (error != null) {
        handleError(error)
      } else if (message != null) {
        requestNext(message)
      }
    }
    webSocket.receiveMessageWithCompletionHandler(completionHandler)
  }

  private fun handleError(error: NSError) {
    messageChannel.close(
        ApolloNetworkException(
            message = "Web socket communication error",
            platformCause = error
        )
    )
    webSocket.cancel()
  }

  private fun requestNext(webSocketMessage: NSURLSessionWebSocketMessage) {
    val data = when (webSocketMessage.type) {
      NSURLSessionWebSocketMessageTypeData -> {
        webSocketMessage.data?.toByteString()?.utf8()
      }

      NSURLSessionWebSocketMessageTypeString -> {
        webSocketMessage.string
      }

      else -> null
    }

    try {
      if (data != null) messageChannel.trySend(data)
    } catch (e: Exception) {
      webSocket.cancel()
      return
    }

    receiveNext()
  }

}


private class NSURLSessionWebSocketDelegate(
    val webSocketConnectionListener: WebSocketConnectionListener,
) : NSObject(), NSURLSessionWebSocketDelegateProtocol {

  override fun URLSession(session: NSURLSession, webSocketTask: NSURLSessionWebSocketTask, didOpenWithProtocol: String?) {
    webSocketConnectionListener.onOpen(webSocketTask)
  }

  override fun URLSession(
      session: NSURLSession,
      webSocketTask: NSURLSessionWebSocketTask,
      didCloseWithCode: NSURLSessionWebSocketCloseCode,
      reason: NSData?,
  ) {
    webSocketConnectionListener.onClose(webSocket = webSocketTask, code = didCloseWithCode)
  }

  override fun URLSession(
      session: NSURLSession,
      task: NSURLSessionTask,
      didCompleteWithError: NSError?
  ) {
    webSocketConnectionListener.onError(error = didCompleteWithError)
  }
}
