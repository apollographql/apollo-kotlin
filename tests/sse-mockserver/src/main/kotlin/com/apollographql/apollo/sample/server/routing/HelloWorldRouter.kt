package com.apollographql.apollo.sample.server.routing

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get

class HelloWorldRouter : Router() {

  companion object {
    const val PAYLOAD_HELLO_WORLD = "Hello, world!"
    const val PATH_HELLO_WORLD = "HelloWorld"
  }

  override fun routing(routing: Routing) {

    routing.get("/${PATH_HELLO_WORLD}") {
      call.respondText(PAYLOAD_HELLO_WORLD)
    }
  }
}
