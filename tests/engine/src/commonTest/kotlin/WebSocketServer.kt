import WebSocketServer.WebSocketEvent.BinaryFrame
import WebSocketServer.WebSocketEvent.Close
import WebSocketServer.WebSocketEvent.Connect
import WebSocketServer.WebSocketEvent.Error
import WebSocketServer.WebSocketEvent.TextFrame
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

class WebSocketServer(private val port: Int = Random.nextInt(10000, 20000)) {
  sealed class WebSocketEvent {
    class Connect(val sessionId: String, val headers: Map<String, String>) : WebSocketEvent()
    class TextFrame(val sessionId: String, val text: String) : WebSocketEvent()
    class BinaryFrame(val sessionId: String, val bytes: ByteArray) : WebSocketEvent()
    class Close(val sessionId: String, val reasonCode: Short?, val reasonMessage: String?) : WebSocketEvent()
    class Error(val sessionId: String, val cause: Throwable) : WebSocketEvent()
  }

  private var server: ApplicationEngine? = null

  private val _events = MutableSharedFlow<WebSocketEvent>()
  val events: Flow<WebSocketEvent> = _events

  fun start() {
    server = embeddedServer(CIO, port) { webSocketServer() }.start(wait = false)
  }

  fun stop() {
    server?.stop(0, 0)
  }

  fun url(): String {
    return "ws://127.0.0.1:$port"
  }

  private class Session(val id: String, val session: DefaultWebSocketSession)

  private val sessions = mutableMapOf<String, Session>()

  suspend fun sendText(sessionId: String, text: String) {
    sessions[sessionId]?.session?.send(text)
  }

  suspend fun sendBinary(sessionId: String, binary: ByteArray) {
    sessions[sessionId]?.session?.send(binary)
  }

  suspend fun sendClose(sessionId: String, reasonCode: Short? = null, reasonMessage: String? = null) {
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
              is Frame.Text -> _events.emit(TextFrame(sessionId, frame.readText()))
              is Frame.Binary -> _events.emit(BinaryFrame(sessionId, frame.readBytes()))
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
