package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.network.toNSData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.cinterop.convert
import okio.ByteString.Companion.toByteString
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
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

internal class AppleWebSocketEngine : WebSocketEngine {
  private val delegate = Delegate()
  private val nsUrlSession = NSURLSession.sessionWithConfiguration(
      configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
      delegate = delegate,
      delegateQueue = NSOperationQueue()
  )

  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    val serverUrl = NSURL(string = url)

    val request = NSMutableURLRequest.requestWithURL(serverUrl).apply {
      headers.forEach { setValue(it.value, forHTTPHeaderField = it.name) }
      setHTTPMethod("GET")
    }

    val task = nsUrlSession.webSocketTaskWithRequest(request)
    val webSocket = AppleWebSocket(task, listener)
    delegate.associate(task, webSocket)
    webSocket.connect()

    return webSocket
  }

  override fun close() {
    delegate.close()
    nsUrlSession.invalidateAndCancel()
  }
}


private class Delegate: NSObject(), NSURLSessionWebSocketDelegateProtocol {
  private val lock = reentrantLock()
  private val map = mutableMapOf<NSURLSessionWebSocketTask, AppleWebSocket>()

  fun associate(webSocketTask: NSURLSessionWebSocketTask, webSocket: AppleWebSocket) {
    lock.withLock {
      map.put(webSocketTask, webSocket)
    }
  }

  override fun URLSession(session: NSURLSession, webSocketTask: NSURLSessionWebSocketTask, didOpenWithProtocol: String?) {
    val webSocket = lock.withLock {
      map.get(webSocketTask)
    }
    webSocket?.onOpen()
  }

  override fun URLSession(
      session: NSURLSession,
      webSocketTask: NSURLSessionWebSocketTask,
      didCloseWithCode: NSURLSessionWebSocketCloseCode,
      reason: NSData?,
  ) {
    val webSocket = lock.withLock {
      val ws = map.get(webSocketTask)

      map.remove(webSocketTask)
      ws
    }
    webSocket?.onClosed(didCloseWithCode.convert(), reason?.toByteString()?.utf8())
  }

  fun close() {
    lock.withLock {
      map.clear()
    }
  }
}

/**
 * Peculiarities of NSURLSesssionWebSocketTask:
 * - cancelWithCloseCode(code) calls didCloseWithCode with the same client code, making it impossible to detect the server close code
 * - sometimes cancelWithCloseCode(code) doesn't send the close frame to the server (https://developer.apple.com/forums/thread/679446)
 * - when the server close frame is received, the received completion handler is called first with an error, making it quite difficult
 * to detect server close
 */
internal class AppleWebSocket(
    private val nsurlSessionWebSocketTask: NSURLSessionWebSocketTask,
    private val listener: WebSocketListener,
) : WebSocket {
  private val disposed = atomic(false)

  internal fun connect() {
    nsurlSessionWebSocketTask.resume()
    receiveNext()
  }

  fun onOpen() {
    listener.onOpen()
  }

  fun onClosed(code: Int?, reason: String?) {
    if (disposed.compareAndSet(expect = false, update = true)) {
      listener.onClosed(code, reason)
    }
  }

  private fun receiveNext() {
    nsurlSessionWebSocketTask.receiveMessageWithCompletionHandler { message, nsError ->
      if (nsError != null) {
        if (disposed.compareAndSet(expect = false, update = true)) {
          listener.onError(DefaultApolloException("Error reading websocket: ${nsError.localizedDescription}"))
        }
      } else if (message != null) {
        when (message.type) {
          NSURLSessionWebSocketMessageTypeData -> {
            listener.onMessage(message.data!!.toByteString().toByteArray())
          }

          NSURLSessionWebSocketMessageTypeString -> {
            listener.onMessage(message.string!!)
          }
        }

        receiveNext()
      }
    }
  }

  override fun send(data: ByteArray) {
    nsurlSessionWebSocketTask.sendMessage(NSURLSessionWebSocketMessage(data = data.toNSData())) {}
  }

  override fun send(text: String) {
    nsurlSessionWebSocketTask.sendMessage(NSURLSessionWebSocketMessage(string = text)) {}
  }

  override fun close(code: Int, reason: String) {
    if (disposed.compareAndSet(expect = false, update = true)) {
      nsurlSessionWebSocketTask.cancelWithCloseCode(code.convert(), reason.encodeToByteArray().toNSData())
    }
  }
}

actual fun WebSocketEngine(): WebSocketEngine = AppleWebSocketEngine()
