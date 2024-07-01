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
import java.io.BufferedReader
import java.io.PrintStream
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
        launch { handleClient(clientSocket) }
      }
    }
  }

  private fun handleClient(clientSocket: LocalSocket) {
    try {
      val bufferedReader = clientSocket.inputStream.bufferedReader()
      val printWriter = PrintStream(clientSocket.outputStream.buffered(), true)
      val httpRequest = readHttpRequest(bufferedReader)
      if (httpRequest.method == "OPTIONS") {
        printWriter.print("HTTP/1.1 204 No Content\r\nConnection: close\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Methods: *\r\nAccess-Control-Allow-Headers: *\r\n\r\n")
        return
      }
      printWriter.print("HTTP/1.1 200 OK\r\nConnection: close\r\nAccess-Control-Allow-Origin: *\r\nContent-Type: application/json\r\n\r\n")
      printWriter.print(graphQL.executeGraphQL(httpRequest.body ?: ""))
    } catch (e: CancellationException) {
      // Expected when the server is closed
      throw e
    } catch (_: Exception) {
      // I/O error or otherwise: ignore
    } finally {
      runCatching { clientSocket.close() }
    }
  }

  private class HttpRequest(
      val method: String,
      val path: String,
      val headers: List<Pair<String, String>>,
      val body: String?,
  )

  private fun readHttpRequest(bufferedReader: BufferedReader): HttpRequest {
    val (method, path) = bufferedReader.readLine().split(" ")
    val headers = mutableListOf<Pair<String, String>>()
    while (true) {
      val line = bufferedReader.readLine()
      if (line.isEmpty()) break
      val (key, value) = line.split(": ")
      headers.add(key to value)
    }
    val contentLength = headers.firstOrNull { it.first.equals("Content-Length", ignoreCase = true) }?.second?.toLongOrNull() ?: 0
    val body = if (contentLength <= 0) {
      null
    } else {
      val buffer = CharArray(contentLength.toInt())
      bufferedReader.read(buffer)
      String(buffer)
    }
    return HttpRequest(method, path, headers, body)
  }

  override fun stop() {
    runCatching { localServerSocket?.close() }
    coroutineScope.cancel()
    dispatcher.close()
  }
}
