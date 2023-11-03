package com.apollographql.apollo3.debugserver.internal.server

import com.apollographql.apollo3.ApolloClient

internal interface Server {
  fun start()
  fun stop()
}

internal expect fun createServer(
    apolloClients: Map<ApolloClient, String>,
): Server

internal class NoOpServer : Server {
  override fun start() {}

  override fun stop() {}
}
