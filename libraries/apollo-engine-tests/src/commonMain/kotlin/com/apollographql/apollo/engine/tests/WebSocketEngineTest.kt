package com.apollographql.apollo.engine.tests

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.network.ws.WebSocketEngine
import com.apollographql.mockserver.CloseFrame
import com.apollographql.mockserver.DataMessage
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.TextMessage
import com.apollographql.mockserver.awaitWebSocketRequest
import com.apollographql.mockserver.enqueueWebSocket
import kotlinx.coroutines.delay
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal enum class Platform {
  Native,
  Js,
  Jvm
}

internal expect fun platform(): Platform

/**
 * NSURLSession has a bug that sometimes skips the close frame
 * See https://developer.apple.com/forums/thread/679446
 */
private fun maySkipCloseFrame(): Boolean {
  return platform() == Platform.Native
}

@ApolloInternal
suspend fun textFrames(webSocketEngine: () -> WebSocketEngine) {
  val webSocketServer = MockServer()

  val responseBody = webSocketServer.enqueueWebSocket()

  val connection = webSocketEngine().open(webSocketServer.url())
  val request = webSocketServer.awaitWebSocketRequest()

  connection.send("client->server")
  request.awaitMessage().apply {
    assertIs<TextMessage>(this)
    assertEquals("client->server", text)
  }

  responseBody.enqueueMessage(TextMessage("server->client"))
  assertEquals("server->client", connection.receive())

  connection.close()
  if (!maySkipCloseFrame()) {
    request.awaitMessage().apply {
      assertIs<CloseFrame>(this)
    }
  }

  webSocketServer.close()
}

@ApolloInternal
suspend fun binaryFrames(webSocketEngine: () -> WebSocketEngine) {
  if (platform() == Platform.Js) return

  val webSocketServer = MockServer()

  val responseBody = webSocketServer.enqueueWebSocket()

  val connection = webSocketEngine().open(webSocketServer.url())
  val request = webSocketServer.awaitWebSocketRequest()

  connection.send("client->server".encodeUtf8())
  request.awaitMessage().apply {
    assertIs<DataMessage>(this)
    assertEquals("client->server", data.decodeToString())
  }

  responseBody.enqueueMessage(DataMessage("server->client".encodeToByteArray()))
  assertEquals("server->client", connection.receive())

  connection.close()
  if (!maySkipCloseFrame()) {
    request.awaitMessage().apply {
      assertIs<CloseFrame>(this)
    }
  }

  webSocketServer.close()
}

@ApolloInternal
suspend fun serverCloseNicely(webSocketEngine: () -> WebSocketEngine, checkCloseCode: Boolean) {
  if (platform() == Platform.Js) return // It's not clear how termination works on JS

  val webSocketServer = MockServer()

  val responseBody = webSocketServer.enqueueWebSocket()
  val connection = webSocketEngine().open(webSocketServer.url())

  // See https://youtrack.jetbrains.com/issue/KTOR-7099/Race-condition-in-darwin-websockets-client-runIncomingProcessor
  delay(1000)

  responseBody.enqueueMessage(CloseFrame(4200, "Bye now"))
  val e = assertFailsWith<ApolloException> {
    connection.receive()
  }

  // On Apple, the close code and reason are not available - https://youtrack.jetbrains.com/issue/KTOR-6198
  if (checkCloseCode) {
    assertTrue(e is ApolloWebSocketClosedException)
    assertEquals(4200, e.code)
    assertEquals("Bye now", e.reason)
  }

  connection.close()
  webSocketServer.close()
}

@ApolloInternal
suspend fun serverCloseAbruptly(webSocketEngine: () -> WebSocketEngine) {
  if (platform() == Platform.Js) return // It's not clear how termination works on JS
  if (platform() == Platform.Native) return // https://youtrack.jetbrains.com/issue/KTOR-6406

  val webSocketServer = MockServer()

  webSocketServer.enqueueWebSocket()
  val connection = webSocketEngine().open(webSocketServer.url())

  webSocketServer.close()

  assertFailsWith<ApolloNetworkException> {
    connection.receive()
  }

  connection.close()
}

@ApolloInternal
suspend fun headers(webSocketEngine: () -> WebSocketEngine) {
  val webSocketServer = MockServer()

  webSocketServer.enqueueWebSocket()

  webSocketEngine().open(webSocketServer.url(), listOf(
      HttpHeader("Sec-WebSocket-Protocol", "graphql-ws"),
      HttpHeader("header1", "value1"),
      HttpHeader("header2", "value2"),
  )
  )

  val request = webSocketServer.awaitWebSocketRequest()

  assertEquals("graphql-ws", request.headers["Sec-WebSocket-Protocol"])
  assertEquals("value1", request.headers["header1"])
  assertEquals("value2", request.headers["header2"])

  webSocketServer.close()
}

