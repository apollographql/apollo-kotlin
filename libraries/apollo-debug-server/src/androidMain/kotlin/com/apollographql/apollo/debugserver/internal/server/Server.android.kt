package com.apollographql.apollo.debugserver.internal.server

import android.net.LocalServerSocket
import android.net.LocalSocket
import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.debugserver.internal.graphql.GraphQL
import com.apollographql.apollo.debugserver.internal.initializer.ApolloDebugServerInitializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

internal actual fun createServer(
    apolloClients: AtomicReference<Map<ApolloClient, String>>,
): Server = AndroidServer(apolloClients)

private class AndroidServer(
    apolloClients: AtomicReference<Map<ApolloClient, String>>,
) : Server {
  companion object {
    private const val SOCKET_NAME_PREFIX = "apollo_debug_"
  }

  private var localServerSocket: LocalServerSocket? = null
  private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
  private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

  private val graphQL = GraphQL(apolloClients)

  override fun start() {
    if (localServerSocket != null) error("Already started")
    val packageName = ApolloDebugServerInitializer.packageName ?: "unknown.${System.currentTimeMillis()}"
    coroutineScope.launch {
      val localServerSocket = try {
        LocalServerSocket("$SOCKET_NAME_PREFIX$packageName")
      } catch (e: Exception) {
        Log.w("ApolloDebugServer", "Could not create server socket", e)
        return@launch
      }
      this@AndroidServer.localServerSocket = localServerSocket
      while (true) {
        val clientSocket = try {
          localServerSocket.accept()
        } catch (_: Exception) {
          // Server socket has been closed (stop() was called)
          break
        }
        launch { handleClient(graphQL, { clientSocket.close() }, clientSocket.inputStream, clientSocket.outputStream) }
      }
    }
  }

  override fun stop() {
    runCatching { localServerSocket?.close() }
    coroutineScope.cancel()
    dispatcher.close()
  }
}
