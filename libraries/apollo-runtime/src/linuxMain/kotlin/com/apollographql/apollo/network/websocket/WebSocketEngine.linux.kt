package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.network.internal.toWebSocketUrl
import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch

@ApolloExperimental
actual fun WebSocketEngine(): WebSocketEngine = LinuxWebSocketEngine()

internal class LinuxWebSocketEngine : WebSocketEngine {
  private val client = HttpClient(Curl) {
    install(WebSockets)
  }

  override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
    return LinuxWebSocket(client, url, headers, listener)
  }

  override fun close() {
    client.close()
  }
}

private class LinuxWebSocket(
    client: HttpClient,
    url: String,
    headers: List<HttpHeader>,
    private val listener: WebSocketListener,
) : WebSocket {
  private val disposed = atomic(false)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val session = atomic<ClientWebSocketSession?>(null)
  private val readyJob = Job()

  init {
    scope.launch {
      val session = try {
        client.webSocketSession(url.toWebSocketUrl()) {
          headers.forEach { header(it.name, it.value) }
        }
      } catch (t: Throwable) {
        readyJob.complete()
        if (disposed.compareAndSet(expect = false, update = true)) {
          listener.onError(ApolloNetworkException("Failed to open WebSocket", t))
        }
        scope.cancel()
        return@launch
      }

      this@LinuxWebSocket.session.value = session
      readyJob.complete()
      listener.onOpen()

      try {
        for (frame in session.incoming) {
          when (frame) {
            is Frame.Text -> listener.onMessage(frame.readText())
            is Frame.Binary -> listener.onMessage(frame.readBytes())
            is Frame.Close -> {
              val reason = frame.readReason()
              if (disposed.compareAndSet(expect = false, update = true)) {
                listener.onClosed(reason?.code?.toInt(), reason?.message)
              }
              return@launch
            }
            else -> Unit
          }
        }
        if (disposed.compareAndSet(expect = false, update = true)) {
          listener.onClosed(null, null)
        }
      } catch (_: ClosedReceiveChannelException) {
        if (disposed.compareAndSet(expect = false, update = true)) {
          listener.onClosed(null, null)
        }
      } catch (t: Throwable) {
        if (disposed.compareAndSet(expect = false, update = true)) {
          listener.onError(ApolloNetworkException("Error reading websocket", t))
        }
      } finally {
        scope.cancel()
      }
    }
  }

  override fun send(data: ByteArray) {
    val s = session.value ?: run {
      if (disposed.compareAndSet(expect = false, update = true)) {
        listener.onError(DefaultApolloException("WebSocket is not connected"))
      }
      return
    }
    scope.launch {
      runCatching { s.send(Frame.Binary(true, data)) }
    }
  }

  override fun send(text: String) {
    val s = session.value ?: run {
      if (disposed.compareAndSet(expect = false, update = true)) {
        listener.onError(DefaultApolloException("WebSocket is not connected"))
      }
      return
    }
    scope.launch {
      runCatching { s.send(Frame.Text(text)) }
    }
  }

  override fun close(code: Int, reason: String) {
    if (!disposed.compareAndSet(expect = false, update = true)) return
    val s = session.value
    if (s == null) {
      scope.cancel()
      return
    }
    scope.launch {
      runCatching { s.close(CloseReason(code.toShort(), reason)) }
      scope.cancel()
    }
  }
}
