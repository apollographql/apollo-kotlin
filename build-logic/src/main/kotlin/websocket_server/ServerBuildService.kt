package websocket_server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.io.files.Path
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.lang.AutoCloseable

private val port = 18923

private fun server(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
  return embeddedServer(Netty, port = port) {
    install(CORS) {
      /**
       * This is very permissive. It's only for tests but feel free to restrict if needed.
       */
      anyMethod()
      anyHost()
      allowHeaders { true }
      allowNonSimpleContentTypes = true
    }
    install(WebSockets)
    routing {
      webSocket("/echo") {
        for (frame in incoming) {
          if (frame is Frame.Text && frame.readText() == "bye") {
            close(CloseReason(4400, "bye"))
          } else {
            if (frame is Frame.Text) {
              send(frame.readText())
            } else {
              send(frame.data)
            }
          }
        }
      }
    }
  }
}

abstract class ServerBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
  private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
  private var refcount = 0

  @Synchronized
  fun startServer() {
    if (refcount == 0) {
      println("Starting echo server")
      server = server().start(wait = false)
    }
    refcount++
  }

  fun stopServer() {
    check(refcount > 0) {
      "Server not started"
    }
    refcount--
    if (refcount == 0) {
      close()
    }
  }

  @Synchronized
  override fun close() {
    if (server != null) {
      println("Stopping echo server")
      server!!.stop(1000, 1000)
      server = null
      refcount = 0
    }
  }
}