package com.apollographql.apollo.sample.server.routing

import io.ktor.routing.Routing

abstract class Router {
  abstract fun routing(routing: Routing)
}
