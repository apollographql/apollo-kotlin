package com.apollographql.apollo3.subscription

import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.testing.MockSubscription
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSyncOperationMessageSerializerTest {
  private val authorization = mapOf(
      "host" to "example1234567890000.appsync-api.us-east-1.amazonaws.com",
      "x-api-key" to "da2-12345678901234567890123456"
  )
  private val serializer = AppSyncOperationMessageSerializer(authorization)

  @Test
  fun writeClientMessage_init() {
    val message = OperationClientMessage.Init(mapOf(
        "param1" to "value1",
        "param2" to "value2"
    ))
    assertEquals(
        serializer.writeClientMessage(message),
        ApolloOperationMessageSerializer.writeClientMessage(message)
    )
  }

  @Test
  fun writeClientMessage_start() {
    val message = OperationClientMessage.Start(
        subscriptionId = "subscription-id",
        subscription = MockSubscription(
            variables = mapOf("variable" to "value"),
            queryDocument = "subscription{commentAdded{id  name}",
            name = "SomeSubscription"
        ),
        responseAdapterCache = ResponseAdapterCache.DEFAULT,
        autoPersistSubscription = false,
        sendSubscriptionDocument = true
    )
    ApolloOperationMessageSerializer.JSON_KEY_VARIABLES
    ApolloOperationMessageSerializer.JSON_KEY_OPERATION_NAME
    ApolloOperationMessageSerializer.JSON_KEY_QUERY
    assertEquals(parseJson(serializer.writeClientMessage(message)), mapOf(
        "id" to message.subscriptionId,
        "type" to "start",
        "payload" to mapOf(
            "data" to """{"variables":{"variable":"value"},"operationName":"SomeSubscription","query":"subscription{commentAdded{id  name}"}""",
            "extensions" to mapOf(
                "authorization" to authorization
            )
        )
    ))
  }

  @Test
  fun writeClientMessage_stop() {
    val message = OperationClientMessage.Stop("subscription-id")
    assertEquals(serializer.writeClientMessage(message), ApolloOperationMessageSerializer.writeClientMessage(message))
  }

  @Test
  fun writeClientMessage_terminate() {
    val message = OperationClientMessage.Terminate()
    assertEquals(serializer.writeClientMessage(message), ApolloOperationMessageSerializer.writeClientMessage(message))
  }

  @Test
  fun readServerMessage_connectionAcknowledge() {
    assertEquals(
        serializer.readServerMessage("""{"type":"connection_ack","payload":{"connectionTimeoutMs":300000}}"""),
        OperationServerMessage.ConnectionAcknowledge
    )
  }

  @Test
  fun readServerMessage_data() {
    assertEquals(
        serializer.readServerMessage("""{"type":"data","id":"some-id","payload":{"key":"value"}}"""),
        OperationServerMessage.Data(
            id = "some-id",
            payload = mapOf("key" to "value")
        )
    )
  }

  @Test
  fun readServerMessage_keepAlive() {
    assertEquals(serializer.readServerMessage("""{"type":"ka"}"""), OperationServerMessage.ConnectionKeepAlive)
  }

  @Test
  fun readServerMessage_error() {
    assertEquals(serializer.readServerMessage("""{"type":"error","id":"some-id","payload":{"key":"value"}}"""), OperationServerMessage.Error(
        id = "some-id",
        payload = mapOf("key" to "value")
    ))
  }

  @Test
  fun readServerMessage_connectionError() {
    assertEquals(serializer.readServerMessage("""{"type":"connection_error","payload":{"key":"value"}}"""), OperationServerMessage.ConnectionError(
        payload = mapOf("key" to "value")
    ))
  }

  @Test
  fun readServerMessage_complete() {
    assertEquals(serializer.readServerMessage("""{"type":"complete","id":"some-id"}"""), OperationServerMessage.Complete(
        id = "some-id"
    ))
  }

  @Test
  fun readServerMessage_unknown() {
    assertEquals(
        serializer.readServerMessage("invalid json"),
        OperationServerMessage.Unsupported("invalid json")
    )
    assertEquals(
        serializer.readServerMessage("{}"),
        OperationServerMessage.Unsupported("{}")
    )
    assertEquals(
        serializer.readServerMessage("""{"type":"unknown"}"""),
        OperationServerMessage.Unsupported("""{"type":"unknown"}""")
    )
  }

  private fun OperationMessageSerializer.writeClientMessage(message: OperationClientMessage): String =
      Buffer()
          .also { writeClientMessage(message, it) }
          .readUtf8()

  private fun OperationMessageSerializer.readServerMessage(json: String): OperationServerMessage =
      Buffer()
          .writeUtf8(json)
          .let { readServerMessage(it) }

  private fun parseJson(json: String): Any? =
      Buffer()
          .writeUtf8(json)
          .let(::BufferedSourceJsonReader)
          .readRecursively()
}