package com.apollographql.apollo.debugserver.internal.server

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.debugserver.internal.graphql.ApolloDebugServerExecutableSchemaBuilder
import com.apollographql.apollo.debugserver.internal.graphql.Query
import com.apollographql.execution.ktor.apolloModule
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import java.util.concurrent.atomic.AtomicReference

internal actual fun createServer(apolloClients: AtomicReference<Map<ApolloClient, String>>): Server = KtorServer(apolloClients)

internal class KtorServer(private val apolloClients: AtomicReference<Map<ApolloClient, String>>) : Server {
  private val server = embeddedServer(CIO, port = 8081) {
    val schema = ApolloDebugServerExecutableSchemaBuilder()
        .queryRoot {
          Query(apolloClients)
        }.build()

    apolloModule(schema)
  }

  override fun start() {
    server.start()
  }

  override fun stop() {
    server.stop()
  }
}