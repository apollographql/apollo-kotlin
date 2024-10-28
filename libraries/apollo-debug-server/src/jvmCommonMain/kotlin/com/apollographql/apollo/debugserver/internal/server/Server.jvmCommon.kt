package com.apollographql.apollo.debugserver.internal.server

import com.apollographql.apollo.debugserver.internal.graphql.GraphQL
import kotlinx.coroutines.CancellationException
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream

internal fun handleClient(graphQL: GraphQL, clientSocket: Closeable, inputStream: InputStream, outputStream: OutputStream) {
  try {
    val bufferedReader = inputStream.bufferedReader()
    val printWriter = PrintStream(outputStream.buffered(), true)
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
