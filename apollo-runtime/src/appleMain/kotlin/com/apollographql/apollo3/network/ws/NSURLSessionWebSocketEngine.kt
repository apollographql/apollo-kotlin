package com.apollographql.apollo3.network.ws

import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.internal.ChannelWrapper
import com.apollographql.apollo3.mpp.assertMainThreadOnNative
import com.apollographql.apollo3.network.toNSData
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import okio.ByteString
import okio.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSError
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
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.NSObject
import platform.darwin.dispatch_async_f
import platform.darwin.dispatch_get_main_queue
import kotlin.native.concurrent.freeze

interface WebSocketConnectionListener {
  fun onOpen(webSocket: NSURLSessionWebSocketTask)

  fun onClose(webSocket: NSURLSessionWebSocketTask, code: NSURLSessionWebSocketCloseCode)
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
      headers: Map<String, String>,
  ): WebSocketConnection {
    assertMainThreadOnNative()

    val serverUrl = NSURL(string = url)

    val request = NSMutableURLRequest.requestWithURL(serverUrl).apply {
      headers.forEach { (key, value) -> setValue(value, forHTTPHeaderField = key) }
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
    assertMainThreadOnNative()
    if (!messageChannel.isClosed) {
      val message = NSURLSessionWebSocketMessage(data.toByteArray().toNSData())
      val webSocketConnectionPtr = StableRef.create(this).asCPointer()
      val completionHandler = { error: NSError? ->
        error?.dispatchOnMain(webSocketConnectionPtr)
        Unit
      }.freeze()
      webSocket.sendMessage(message, completionHandler)
    }
  }

  override fun send(string: String) {
    assertMainThreadOnNative()
    if (!messageChannel.isClosed) {
      val message = NSURLSessionWebSocketMessage(string)
      val webSocketConnectionPtr = StableRef.create(this).asCPointer()
      val completionHandler = { error: NSError? ->
        error?.dispatchOnMain(webSocketConnectionPtr)
        Unit
      }.freeze()
      webSocket.sendMessage(message, completionHandler)
    }
  }

  override fun close() {
    assertMainThreadOnNative()
    messageChannel.close()
  }

  fun receiveNext() {
    assertMainThreadOnNative()

    val webSocketConnectionPtr = StableRef.create(this).asCPointer()
    val completionHandler = { message: NSURLSessionWebSocketMessage?, error: NSError? ->
      error?.dispatchOnMain(webSocketConnectionPtr) ?: message?.dispatchOnMainAndRequestNext(webSocketConnectionPtr)
      Unit
    }
    webSocket.receiveMessageWithCompletionHandler(completionHandler)
  }
}

@Suppress("NAME_SHADOWING")
private fun NSError.dispatchOnMain(webSocketConnectionPtr: COpaquePointer) {
  if (NSThread.isMainThread) {
    dispatch(webSocketConnectionPtr)
  } else {
    dispatch_async_f(
        queue = dispatch_get_main_queue(),
        context = StableRef.create(freeze() to webSocketConnectionPtr).asCPointer(),
        work = staticCFunction { ptr ->
          val errorAndWebSocketConnectionRef = ptr!!.asStableRef<Pair<NSError, COpaquePointer>>()

          val (error, webSocketConnectionPtr) = errorAndWebSocketConnectionRef.get()
          errorAndWebSocketConnectionRef.dispose()

          error.dispatch(webSocketConnectionPtr)
        }
    )
  }
}

private fun NSError.dispatch(webSocketConnectionPtr: COpaquePointer) {
  val webSocketConnectionRef = webSocketConnectionPtr.asStableRef<WebSocketConnectionImpl>()
  val webSocketConnection = webSocketConnectionRef.get()
  webSocketConnectionRef.dispose()

  webSocketConnection.messageChannel.close(
      ApolloNetworkException(
          message = "Web socket communication error",
          platformCause = this
      )
  )
  webSocketConnection.webSocket.cancel()
}

@Suppress("NAME_SHADOWING")
private fun NSURLSessionWebSocketMessage.dispatchOnMainAndRequestNext(webSocketConnectionPtr: COpaquePointer) {
  if (NSThread.isMainThread) {
    dispatchAndRequestNext(webSocketConnectionPtr)
  } else {
    dispatch_async_f(
        queue = dispatch_get_main_queue(),
        context = StableRef.create(freeze() to webSocketConnectionPtr).asCPointer(),
        work = staticCFunction { ptr ->
          val messageAndWebSocketConnectionRef = ptr!!.asStableRef<Pair<NSURLSessionWebSocketMessage, COpaquePointer>>()

          val (message, webSocketConnectionPtr) = messageAndWebSocketConnectionRef.get()
          messageAndWebSocketConnectionRef.dispose()

          message.dispatchAndRequestNext(webSocketConnectionPtr)
        }
    )
  }
}

private fun NSURLSessionWebSocketMessage.dispatchAndRequestNext(webSocketConnectionPtr: COpaquePointer) {
  val webSocketConnectionRef = webSocketConnectionPtr.asStableRef<WebSocketConnectionImpl>()
  val webSocketConnection = webSocketConnectionRef.get()
  webSocketConnectionRef.dispose()

  val data = when (type) {
    NSURLSessionWebSocketMessageTypeData -> {
      data?.toByteString()?.utf8()
    }

    NSURLSessionWebSocketMessageTypeString -> {
      string
    }

    else -> null
  }

  try {
    if (data != null) webSocketConnection.messageChannel.trySend(data)
  } catch (e: Exception) {
    webSocketConnection.webSocket.cancel()
    return
  }

  webSocketConnection.receiveNext()
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
}
