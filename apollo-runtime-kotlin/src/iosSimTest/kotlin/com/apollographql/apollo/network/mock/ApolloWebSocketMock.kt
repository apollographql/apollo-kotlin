package com.apollographql.apollo.network.mock

import com.apollographql.apollo.network.GraphQLRequest
import com.apollographql.apollo.network.GraphQLResponse
import com.apollographql.apollo.network.toNSData
import com.apollographql.apollo.network.websocket.ApolloGraphQLClientMessage
import com.apollographql.apollo.network.websocket.WebSocketConnectionListener
import com.apollographql.apollo.network.websocket.NSWebSocketFactory
import okio.internal.commonAsUtf8ToByteArray
import okio.toByteString
import platform.Foundation.NSError
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSessionWebSocketMessage
import platform.Foundation.NSURLSessionWebSocketTask
import kotlin.test.assertEquals
import kotlin.test.fail

@Suppress("EXPERIMENTAL_API_USAGE")
class NSWebSocketFactoryMock(
    private val expectedRequest: GraphQLRequest,
    private val expectedResponseOnStart: GraphQLResponse
) : NSWebSocketFactory {
  lateinit var lastSessionWebSocketTask: ApolloSessionWebSocketTaskMock

  override fun invoke(request: NSURLRequest, connectionListener: WebSocketConnectionListener): NSURLSessionWebSocketTask {
    return ApolloSessionWebSocketTaskMock(
        expectedRequest = expectedRequest,
        expectedResponseOnStart = expectedResponseOnStart,
        connectionListener = connectionListener
    ).also { lastSessionWebSocketTask = it }
  }
}

@Suppress("EXPERIMENTAL_API_USAGE")
class ApolloSessionWebSocketTaskMock(
    private val expectedRequest: GraphQLRequest,
    private val expectedResponseOnStart: GraphQLResponse,
    private val connectionListener: WebSocketConnectionListener
) : NSURLSessionWebSocketTask() {
  private var receiveMessageCompletionHandler: (NSURLSessionWebSocketMessage?, NSError?) -> Unit = { _, _ -> }
  private var connectionInitSent = false
  private var startSent = false
  private var stopSent = false

  override fun resume() {
    connectionListener.onOpen(this)
  }

  override fun receiveMessageWithCompletionHandler(completionHandler: (NSURLSessionWebSocketMessage?, NSError?) -> Unit) {
    receiveMessageCompletionHandler = completionHandler
  }

  override fun sendMessage(message: NSURLSessionWebSocketMessage, completionHandler: (NSError?) -> Unit) {
    when {
      !connectionInitSent -> {
        assertEquals(ApolloGraphQLClientMessage.Init(emptyMap()).serialize(), message.data!!.toByteString())
        connectionInitSent = true
        completionHandler(null)

        receiveMessageCompletionHandler(
            NSURLSessionWebSocketMessage(
                "{\"type\": \"connection_ack\"}".commonAsUtf8ToByteArray().toNSData()
            ),
            null
        )
      }

      !startSent -> {
        assertEquals(ApolloGraphQLClientMessage.Start(expectedRequest).serialize(), message.data!!.toByteString())
        startSent = true

        completionHandler(null)

        receiveMessageCompletionHandler(
            NSURLSessionWebSocketMessage(expectedResponseOnStart.body.readByteArray().toNSData()),
            null
        )
      }

      !stopSent -> {
        assertEquals(ApolloGraphQLClientMessage.Stop(expectedRequest.uuid).serialize(), message.data!!.toByteString())
        stopSent = true
      }

      else -> fail("Unexpected client message: `${message.data!!.toByteString().utf8()}`")
    }
  }

  fun enqueueResponse(response: GraphQLResponse) {
    receiveMessageCompletionHandler(
        NSURLSessionWebSocketMessage(response.body.readByteArray().toNSData()),
        null
    )
  }

  fun enqueueComplete() {
    receiveMessageCompletionHandler(
        NSURLSessionWebSocketMessage(
            "{\"type\": \"complete\", \"id\":\"${expectedRequest.uuid}\"}".commonAsUtf8ToByteArray().toNSData()
        ),
        null
    )
  }
}
