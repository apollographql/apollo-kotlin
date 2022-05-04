package au.com.woolworths.sample.graphqlsse.server

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

open class KtorServerInteractor(val port: Int = 8080) {
    open fun invoke() {
        println("Starting server")

        embeddedServer(Netty, port = port, watchPaths = listOf("classes")) {

            routing {
              get("/$PATH_HELLO_WORLD") {
                call.respondText(KtorServerInteractor.PAYLOAD_HELLO_WORLD)
              }
            }
        }.start()
    }

    companion object {
        val PAYLOAD_HELLO_WORLD = "Hello, world!"
        val PATH_HELLO_WORLD = "helloworld"
    }
}
