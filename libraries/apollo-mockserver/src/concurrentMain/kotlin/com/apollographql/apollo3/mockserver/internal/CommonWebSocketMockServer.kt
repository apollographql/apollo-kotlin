package com.apollographql.apollo3.mockserver.internal

import com.apollographql.apollo3.mockserver.WebSocketMockServer
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent.BinaryMessage
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent.Close
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent.Connect
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent.Error
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent.TextMessage
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.util.toMap
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.random.Random

internal class CommonWebSocketMockServer(private val port: Int) : WebSocketMockServer {
  private var server: ApplicationEngine? = null

  private val _events = MutableSharedFlow<WebSocketEvent>()
  override val events: Flow<WebSocketEvent> = _events

  override fun start() {
    check(server == null) { "Server already started" }
    server = embeddedServer(CIO, port) { webSocketServer() }.start(wait = false)
  }

  override fun close() {
    server?.stop(100, 100)
  }

  override suspend fun url(): String {
    val actualPort = server!!.resolvedConnectors().first().port
    return "ws://127.0.0.1:$actualPort"
  }

  private class Session(val id: String, val session: DefaultWebSocketSession)

  private val sessions = mutableMapOf<String, Session>()

  override suspend fun sendText(sessionId: String, text: String) {
    sessions[sessionId]?.session?.send(text)
  }

  override suspend fun sendBinary(sessionId: String, binary: ByteArray) {
    sessions[sessionId]?.session?.send(binary)
  }

  override suspend fun sendClose(sessionId: String, reasonCode: Short?, reasonMessage: String?) {
    sessions[sessionId]?.session?.close(CloseReason(reasonCode ?: CloseReason.Codes.NORMAL.code, reasonMessage ?: ""))
  }


  private fun Application.webSocketServer() {
    install(WebSockets)
    routing {
      webSocket("/") {
        val sessionId = Random.nextInt().toString()
        sessions[sessionId] = Session(sessionId, this)
        try {
          _events.emit(Connect(sessionId = sessionId, headers = call.request.headers.toMap().mapValues { it.value.first() }))
          for (frame in incoming) {
            when (frame) {
              is Frame.Text -> _events.emit(TextMessage(sessionId, frame.readText()))
              is Frame.Binary -> _events.emit(BinaryMessage(sessionId, frame.readBytes()))
              else -> {}
            }
          }
          val closeReason = closeReason.await()
          _events.emit(Close(sessionId, closeReason?.code, closeReason?.message))
        } catch (e: ClosedReceiveChannelException) {
          val closeReason = closeReason.await()
          _events.emit(Close(sessionId, closeReason?.code, closeReason?.message))
        } catch (e: Throwable) {
          _events.emit(Error(sessionId, e))
        } finally {
          sessions.remove(sessionId)
        }
      }
    }
  }
}
