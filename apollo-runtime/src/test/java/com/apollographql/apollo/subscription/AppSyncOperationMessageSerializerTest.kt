package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import com.google.common.truth.Truth.assertThat
import okio.Buffer
import org.junit.Test

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
    assertThat(serializer.writeClientMessage(message)).isEqualTo(ApolloOperationMessageSerializer.writeClientMessage(message))
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
        customScalarAdapters = CustomScalarAdapters.DEFAULT,
        autoPersistSubscription = false,
        sendSubscriptionDocument = true
    )
    ApolloOperationMessageSerializer.JSON_KEY_VARIABLES
    ApolloOperationMessageSerializer.JSON_KEY_OPERATION_NAME
    ApolloOperationMessageSerializer.JSON_KEY_QUERY
    assertThat(parseJson(serializer.writeClientMessage(message))).isEqualTo(mapOf(
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
    assertThat(serializer.writeClientMessage(message)).isEqualTo(ApolloOperationMessageSerializer.writeClientMessage(message))
  }

  @Test
  fun writeClientMessage_terminate() {
    val message = OperationClientMessage.Terminate()
    assertThat(serializer.writeClientMessage(message)).isEqualTo(ApolloOperationMessageSerializer.writeClientMessage(message))
  }

  @Test
  fun readServerMessage_connectionAcknowledge() {
    assertThat(serializer.readServerMessage("""{"type":"connection_ack","payload":{"connectionTimeoutMs":300000}}"""))
        .isEqualTo(OperationServerMessage.ConnectionAcknowledge())
  }

  @Test
  fun readServerMessage_data() {
    assertThat(serializer.readServerMessage("""{"type":"data","id":"some-id","payload":{"key":"value"}}""")).isEqualTo(OperationServerMessage.Data(
        id = "some-id",
        payload = mapOf("key" to "value")
    ))
  }

  @Test
  fun readServerMessage_keepAlive() {
    assertThat(serializer.readServerMessage("""{"type":"ka"}""")).isEqualTo(OperationServerMessage.ConnectionKeepAlive())
  }

  @Test
  fun readServerMessage_error() {
    assertThat(serializer.readServerMessage("""{"type":"error","id":"some-id","payload":{"key":"value"}}""")).isEqualTo(OperationServerMessage.Error(
        id = "some-id",
        payload = mapOf("key" to "value")
    ))
  }

  @Test
  fun readServerMessage_connectionError() {
    assertThat(serializer.readServerMessage("""{"type":"connection_error","payload":{"key":"value"}}""")).isEqualTo(OperationServerMessage.ConnectionError(
        payload = mapOf("key" to "value")
    ))
  }

  @Test
  fun readServerMessage_complete() {
    assertThat(serializer.readServerMessage("""{"type":"complete","id":"some-id"}""")).isEqualTo(OperationServerMessage.Complete(
        id = "some-id"
    ))
  }

  @Test
  fun readServerMessage_unknown() {
    assertThat(serializer.readServerMessage("invalid json"))
        .isEqualTo(OperationServerMessage.Unsupported("invalid json"))
    assertThat(serializer.readServerMessage("{}"))
        .isEqualTo(OperationServerMessage.Unsupported("{}"))
    assertThat(serializer.readServerMessage("""{"type":"unknown"}"""))
        .isEqualTo(OperationServerMessage.Unsupported("""{"type":"unknown"}"""))
  }

  @Test
  fun buildWebSocketUrl() {
    val url = AppSyncOperationMessageSerializer.buildWebSocketUrl(
        baseWebSocketUrl = "wss://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
        authorization = authorization
    )
    assertThat(url).isEqualTo("wss://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql?header=eyJob3N0IjoiZXhhbXBsZTEyMzQ1Njc4OTAwMDAuYXBwc3luYy1hcGkudXMtZWFzdC0xLmFtYXpvbmF3cy5jb20iLCJ4LWFwaS1rZXkiOiJkYTItMTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTYifQ%3D%3D&payload=e30%3D")
  }

  private fun OperationMessageSerializer.writeClientMessage(message: OperationClientMessage): String =
      Buffer()
          .also { writeClientMessage(message, it) }
          .readUtf8()

  private fun OperationMessageSerializer.readServerMessage(json: String): OperationServerMessage =
      Buffer()
          .writeUtf8(json)
          .let { readServerMessage(it) }

  private fun parseJson(json: String): Map<String, Any?>? =
      Buffer()
          .writeUtf8(json)
          .let(::BufferedSourceJsonReader)
          .let(::ResponseJsonStreamReader)
          .toMap()
}