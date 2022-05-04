package au.com.woolworths.sample.graphqlsse.server

import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

open class KtorServerInteractor(private val port: Int = 8080) {

    companion object {
        const val PAYLOAD_HELLO_WORLD = "Hello, world!"
        const val PATH_HELLO_WORLD = "HelloWorld"
    }

  fun invoke() {

    embeddedServer(Netty, port = port, watchPaths = listOf("classes")) {
      routing {
        get("/$PATH_HELLO_WORLD") {
          call.respondText(PAYLOAD_HELLO_WORLD)
        }
      }
    }.start(wait = true)
  }
}
