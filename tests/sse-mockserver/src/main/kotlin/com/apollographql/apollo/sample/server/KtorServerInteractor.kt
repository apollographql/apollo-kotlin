package com.apollographql.apollo.sample.server

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

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
