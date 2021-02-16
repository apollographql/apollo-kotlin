package com.apollographql.apollo3.subscription

import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import com.apollographql.apollo3.api.internal.json.use
import com.apollographql.apollo3.api.internal.json.writeObject
import com.apollographql.apollo3.subscription.ApolloOperationMessageSerializer.writePayloadContentsTo
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException
import okio.use as okioUse

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
  override fun writeClientMessage(message: OperationClientMessage, sink: BufferedSink) {
    when (message) {
      is OperationClientMessage.Start -> JsonWriter.of(sink).use { message.writeTo(it) }
      is OperationClientMessage.Init,
      is OperationClientMessage.Stop,
      is OperationClientMessage.Terminate -> ApolloOperationMessageSerializer.writeClientMessage(message, sink)
    }
  }

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
      Buffer().okioUse { buffer ->
        JsonWriter.of(buffer).use { dataWriter ->
          dataWriter.writeObject { writePayloadContentsTo(dataWriter) }
        }
        buffer.readUtf8()
      }

  companion object {
    private const val JSON_KEY_DATA = "data"
  }
}