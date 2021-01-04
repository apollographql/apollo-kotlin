package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import com.google.common.truth.Truth.assertThat
import okio.Buffer
import org.junit.Test

class ApolloOperationMessageSerializerTest {
  private val serializer = ApolloOperationMessageSerializer

  @Test
  fun writeClientMessage_init() {
    val message = OperationClientMessage.Init(mapOf(
        "param1" to "value1",
        "param2" to "value2"
    ))
    assertThat(parseJson(serializer.writeClientMessage(message))).isEqualTo(mapOf(
        "type" to "connection_init",
        "payload" to message.connectionParams
    ))
  }

  @Test
  fun writeClientMessage_start() {
    val subscription = MockSubscription(
        variables = mapOf("variable" to "value"),
        queryDocument = "subscription{commentAdded{id  name}",
        name = "SomeSubscription"
    )
    val regularQuery = OperationClientMessage.Start(
        subscriptionId = "subscription-id",
        subscription = subscription,
        customScalarAdapters = CustomScalarAdapters.DEFAULT,
        autoPersistSubscription = false,
        sendSubscriptionDocument = true
    )
    val persistedQueryWithoutDocument = OperationClientMessage.Start(
        subscriptionId = "subscription-id",
        subscription = subscription,
        customScalarAdapters = CustomScalarAdapters.DEFAULT,
        autoPersistSubscription = true,
        sendSubscriptionDocument = false
    )
    val persistedQueryWithDocument = OperationClientMessage.Start(
        subscriptionId = "subscription-id",
        subscription = subscription,
        customScalarAdapters = CustomScalarAdapters.DEFAULT,
        autoPersistSubscription = true,
        sendSubscriptionDocument = true
    )
    assertThat(parseJson(serializer.writeClientMessage(regularQuery))).isEqualTo(mapOf(
        "id" to regularQuery.subscriptionId,
        "type" to "start",
        "payload" to mapOf(
            "variables" to subscription.variables().valueMap(),
            "operationName" to subscription.name().name(),
            "query" to subscription.queryDocument()
        )
    ))
    assertThat(parseJson(serializer.writeClientMessage(persistedQueryWithoutDocument))).isEqualTo(mapOf(
        "id" to persistedQueryWithoutDocument.subscriptionId,
        "type" to "start",
        "payload" to mapOf(
            "variables" to subscription.variables().valueMap(),
            "operationName" to subscription.name().name(),
            "extensions" to mapOf(
                "persistedQuery" to mapOf(
                    "version" to BigDecimal(1),
                    "sha256Hash" to subscription.operationId()
                )
            )
        )
    ))
    assertThat(parseJson(serializer.writeClientMessage(persistedQueryWithDocument))).isEqualTo(mapOf(
        "id" to persistedQueryWithDocument.subscriptionId,
        "type" to "start",
        "payload" to mapOf(
            "variables" to subscription.variables().valueMap(),
            "operationName" to subscription.name().name(),
            "query" to subscription.queryDocument(),
            "extensions" to mapOf(
                "persistedQuery" to mapOf(
                    "version" to BigDecimal(1),
                    "sha256Hash" to subscription.operationId()
                )
            )
        )
    ))
  }

  @Test
  fun writeClientMessage_stop() {
    val message = OperationClientMessage.Stop("subscription-id")
    assertThat(parseJson(serializer.writeClientMessage(message))).isEqualTo(mapOf(
        "type" to "stop",
        "id" to "subscription-id"
    ))
  }

  @Test
  fun writeClientMessage_terminate() {
    val message = OperationClientMessage.Terminate()
    assertThat(parseJson(serializer.writeClientMessage(message))).isEqualTo(mapOf(
        "type" to "connection_terminate"
    ))
  }

  @Test
  fun readServerMessage_connectionAcknowledge() {
    assertThat(serializer.readServerMessage("""{"type":"connection_ack"}"""))
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