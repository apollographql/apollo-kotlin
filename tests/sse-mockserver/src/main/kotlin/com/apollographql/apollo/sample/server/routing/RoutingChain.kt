package com.apollographql.apollo.sample.server.routing

import io.ktor.routing.*

class RoutingChain(private val routerList: List<Router>) : Router() {
    override fun routing(routing: Routing) {

        routerList
            .forEach {
                it.routing(routing)
            }
    }

}
