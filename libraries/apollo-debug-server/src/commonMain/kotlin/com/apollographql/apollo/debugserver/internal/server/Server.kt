package com.apollographql.apollo.debugserver.internal.server

import com.apollographql.apollo.ApolloClient
import java.util.concurrent.atomic.AtomicReference

internal interface Server {
  fun start()
  fun stop()
}

internal expect fun createServer(
    apolloClients: AtomicReference<Map<ApolloClient, String>>,
): Server

internal class NoOpServer : Server {
  override fun start() {}

  override fun stop() {}
}
