package com.apollographql.apollo.debugserver.internal.server

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.debugserver.internal.graphql.GraphQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

internal actual fun createServer(
    apolloClients: AtomicReference<Map<ApolloClient, String>>,
): Server = JvmServer(apolloClients)

private class JvmServer(
    apolloClients: AtomicReference<Map<ApolloClient, String>>,
) : Server {
  private var serverSocket: ServerSocket? = null
  private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
  private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

  private val graphQL = GraphQL(apolloClients)

  override fun start() {
    if (serverSocket != null) error("Already started")
    coroutineScope.launch {
      val localServerSocket = ServerSocket(8081)
      this@JvmServer.serverSocket = localServerSocket
      while (true) {
        val clientSocket = try {
          localServerSocket.accept()
        } catch (_: Exception) {
          // Server socket has been closed (stop() was called)
          break
        }
        launch { handleClient(graphQL, clientSocket, clientSocket.inputStream, clientSocket.outputStream) }
      }
    }
  }

  override fun stop() {
    runCatching { serverSocket?.close() }
    coroutineScope.cancel()
    dispatcher.close()
  }
}
