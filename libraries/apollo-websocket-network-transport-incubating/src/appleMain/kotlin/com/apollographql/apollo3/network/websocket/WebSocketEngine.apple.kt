package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.network.toNSData
import kotlinx.atomicfu.atomic
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

class AppleWebSocketEngine : WebSocketEngine {
  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    return AppleWebSocket(url, headers, listener)
  }
}

/**
 * Peculiarities of NSURLSesssionWebSocketTask:
 * - cancelWithCloseCode(code) calls didCloseWithCode with the same client code, making it impossible to detect the server close code
 * - sometimes cancelWithCloseCode(code) doesn't send the close frame to the server (https://developer.apple.com/forums/thread/679446)
 * - when the server close frame is received, the receive completion handler is called first with an error, making it quite difficult
 * to detect server close
 */
class AppleWebSocket(
    private val url: String,
    private val headers: List<HttpHeader>,
    private val listener: WebSocketListener,
) : WebSocket {
  private lateinit var nsurlSessionWebSocketTask: NSURLSessionWebSocketTask
  private val disposed = atomic(false)

  inner class Delegate: NSObject(), NSURLSessionWebSocketDelegateProtocol {
    override fun URLSession(session: NSURLSession, webSocketTask: NSURLSessionWebSocketTask, didOpenWithProtocol: String?) {
      listener.onOpen()
    }

    override fun URLSession(
        session: NSURLSession,
        webSocketTask: NSURLSessionWebSocketTask,
        didCloseWithCode: NSURLSessionWebSocketCloseCode,
        reason: NSData?,
    ) {
      if (disposed.compareAndSet(expect = false, update = true)) {
        listener.onClosed(didCloseWithCode.convert(), reason?.toByteString()?.utf8())
      }
    }
  }

  override fun connect() {
    val serverUrl = NSURL(string = url)

    val request = NSMutableURLRequest.requestWithURL(serverUrl).apply {
      headers.forEach { setValue(it.value, forHTTPHeaderField = it.name) }
      setHTTPMethod("GET")
    }

    nsurlSessionWebSocketTask = NSURLSession.sessionWithConfiguration(
        configuration = NSURLSessionConfiguration.defaultSessionConfiguration,
        delegate = Delegate(),
        delegateQueue = NSOperationQueue.currentQueue()
    ).webSocketTaskWithRequest(request)

    nsurlSessionWebSocketTask.resume()

    receiveNext()
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
    check(::nsurlSessionWebSocketTask.isInitialized) {
      "You must call connect() before send()"
    }

    nsurlSessionWebSocketTask.sendMessage(NSURLSessionWebSocketMessage(data = data.toNSData())) {}
  }

  override fun send(text: String) {
    check(::nsurlSessionWebSocketTask.isInitialized) {
      "You must call connect() before send()"
    }
    nsurlSessionWebSocketTask.sendMessage(NSURLSessionWebSocketMessage(string = text)) {}
  }

  override fun close(code: Int, reason: String) {
    check(::nsurlSessionWebSocketTask.isInitialized) {
      "You must call connect() before close()"
    }
    if (disposed.compareAndSet(expect = false, update = true)) {
      nsurlSessionWebSocketTask.cancelWithCloseCode(code.convert(), reason.encodeToByteArray().toNSData())
    }
  }
}

actual fun WebSocketEngine(): WebSocketEngine = AppleWebSocketEngine()
