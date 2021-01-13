package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.writeObject
import com.apollographql.apollo.subscription.ApolloOperationMessageSerializer.writePayloadContentsTo
import okhttp3.HttpUrl
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

/**
 * An [OperationMessageSerializer] that uses the format used by
 * [AWS AppSync][https://docs.aws.amazon.com/appsync/latest/devguide/real-time-websocket-client.html].
 *
 * AppSync uses a dialect of Apollo's format so parts of the implementation is delegated to [ApolloOperationMessageSerializer].
 *
 * @param authorization The Authorization object as per the AWS AppSync documentation.
 */
class AppSyncOperationMessageSerializer(
    private val authorization: Map<String, Any?>
) : OperationMessageSerializer {
  @Throws(IOException::class)
  override fun writeClientMessage(message: OperationClientMessage, sink: BufferedSink) {
    when (message) {
      is OperationClientMessage.Start -> JsonWriter.of(sink).use { message.writeTo(it) }
      is OperationClientMessage.Init,
      is OperationClientMessage.Stop,
      is OperationClientMessage.Terminate -> ApolloOperationMessageSerializer.writeClientMessage(message, sink)
    }
  }

  @Throws(IOException::class)
  override fun readServerMessage(source: BufferedSource): OperationServerMessage =
      ApolloOperationMessageSerializer.readServerMessage(source)

  private fun OperationClientMessage.Start.writeTo(writer: JsonWriter) {
    writer.writeObject {
      name(ApolloOperationMessageSerializer.JSON_KEY_ID).value(subscriptionId)
      name(ApolloOperationMessageSerializer.JSON_KEY_TYPE).value(OperationClientMessage.Start.TYPE)
      name(ApolloOperationMessageSerializer.JSON_KEY_PAYLOAD).writeObject {
        name(JSON_KEY_DATA).value(serializePayload())
        name(ApolloOperationMessageSerializer.JSON_KEY_EXTENSIONS).writeObject {
          name("authorization")
          Utils.writeToJson(authorization, writer)
        }
      }
    }
  }

  private fun OperationClientMessage.Start.serializePayload(): String =
      Buffer().use { buffer ->
        JsonWriter.of(buffer).use { dataWriter ->
          dataWriter.writeObject { writePayloadContentsTo(dataWriter) }
        }
        buffer.readUtf8()
      }

  companion object {
    private const val JSON_KEY_DATA = "data"

    /**
     * Helper method that builds the final web socket URL. It will append the authorization and payload arguments as query parameters.
     *
     * Example:
     * ```
     * buildWebSocketUrl(
     *   baseWebSocketUrl = "wss://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
     *   // This example uses an API key. See the AppSync documentation for information on what to pass
     *   authorization = mapOf(
     *     "host" to "example1234567890000.appsync-api.us-east-1.amazonaws.com",
     *     "x-api-key" to "da2-12345678901234567890123456"
     *   )
     * )
     * ```
     *
     * @param baseWebSocketUrl The base web socket URL.
     * @param authorization The authorization as per the AppSync documentation.
     * @param payload An optional payload. Defaults to an empty object.
     */
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
      return buffer.readByteString().base64Url()
    }
  }
}