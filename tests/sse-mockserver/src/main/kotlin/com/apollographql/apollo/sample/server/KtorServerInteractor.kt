package com.apollographql.apollo.sample.server

import com.apollographql.apollo.sample.server.routing.HelloWorldRouter
import com.apollographql.apollo.sample.server.routing.RoutingChain
import com.apollographql.apollo.sample.server.routing.SseSideChannelRouter
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json

open class KtorServerInteractor(private val port: Int = 8080) {

  fun invoke() {

    embeddedServer(Netty, port = port, watchPaths = listOf("classes")) {

      install(ContentNegotiation) {
        json(Json {
          prettyPrint = true
          isLenient = true
        })

      }

      routing {
        listOf(
            HelloWorldRouter(),
            SseSideChannelRouter(),
        )
            .let { RoutingChain(it) }
            .routing(this)
      }
    }.start(wait = true)
  }
}
