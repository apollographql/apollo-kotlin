package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.writeObject
import com.apollographql.apollo.subscription.ApolloOperationMessageSerializer.writePayloadContentsTo
import okhttp3.HttpUrl
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import java.net.URL
import java.util.Base64

class AppSyncOperationMessageSerializer(
    private val authorization: Map<String, Any?>
) : OperationMessageSerializer {
  override fun OperationClientMessage.writeTo(sink: BufferedSink) {
    when (this) {
      is OperationClientMessage.Start -> JsonWriter.of(sink).use { writeTo(it) }
      is OperationClientMessage.Init,
      is OperationClientMessage.Stop,
      is OperationClientMessage.Terminate -> with(ApolloOperationMessageSerializer) { writeTo(sink) }
    }
  }

  override fun readServerMessage(source: BufferedSource): OperationServerMessage =
      ApolloOperationMessageSerializer.readServerMessage(source)

  private fun OperationClientMessage.Start.writeTo(writer: JsonWriter) {
    writer.writeObject {
      name(ApolloOperationMessageSerializer.JSON_KEY_ID).value(subscriptionId)
      name(ApolloOperationMessageSerializer.JSON_KEY_TYPE).value(OperationClientMessage.Start.TYPE)
      name(ApolloOperationMessageSerializer.JSON_KEY_PAYLOAD).writeObject {
        name(JSON_KEY_DATA).writeObject {
          writePayloadContentsTo(writer)
        }

        if (autoPersistSubscription) {
          name(ApolloOperationMessageSerializer.JSON_KEY_EXTENSIONS).writeObject {
            name("authorization")
            Utils.writeToJson(authorization, writer)
          }
        }
      }
    }
  }

  companion object {
    private const val JSON_KEY_DATA = "data"

    @JvmOverloads
    @JvmStatic
    fun buildWebSocketUrl(
        baseWebSocketUrl: String,
        authorization: Map<String, Any?>,
        payload: Map<String, Any?> = emptyMap(),
    ): String =
        baseWebSocketUrl
          .let { url ->
            when {
              url.startsWith("ws://", ignoreCase = true) -> "http" + url.drop(2)
              url.startsWith("wss://", ignoreCase = true) -> "https" + url.drop(3)
              else -> url
            }
          }
          .let { HttpUrl.get(it) }
          .newBuilder()
          .setQueryParameter("header", authorization.base64Encode())
          .setQueryParameter("payload", payload.base64Encode())
          .build()
          .toString()
          .let { url ->
            when {
              url.startsWith("http://", ignoreCase = true) -> "ws" + url.drop(4)
              url.startsWith("https://", ignoreCase = true) -> "wss" + url.drop(5)
              else -> url
            }
          }

    private fun Map<String, Any?>.base64Encode(): String {
      val buffer = Buffer()
      Utils.writeToJson(this, JsonWriter.of(buffer))
      return Base64.getUrlEncoder().encodeToString(buffer.readByteArray())
    }
  }
}