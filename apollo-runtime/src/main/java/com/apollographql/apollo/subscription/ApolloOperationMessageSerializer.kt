package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonEncodingException
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.writeObject
import okio.BufferedSink
import okio.BufferedSource
import java.io.IOException
import java.util.Collections

/**
 * An [OperationMessageSerializer] that uses the standard
 * [Apollo format][https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md].
 */
object ApolloOperationMessageSerializer : OperationMessageSerializer {
  @Throws(IOException::class)
  override fun writeClientMessage(message: OperationClientMessage, sink: BufferedSink) {
    JsonWriter.of(sink).use { writer ->
      writer.writeObject {
        message.writeContentsTo(writer)
      }
    }
  }

  @Throws(IOException::class)
  override fun readServerMessage(source: BufferedSource): OperationServerMessage =
      try {
        source.peek().use {
          BufferedSourceJsonReader(it).use { reader ->
            reader.readServerMessage()
          }
        }
      } catch (e: JsonEncodingException) {
        OperationServerMessage.Unsupported(source.readUtf8())
      } catch (e: IOException) {
        // Rethrow IOExceptions as they don't mean the data was invalid
        throw e
      } catch (e: Exception) {
        OperationServerMessage.Unsupported(source.readUtf8())
      }

  internal fun OperationClientMessage.writeContentsTo(writer: JsonWriter) {
    when (this@writeContentsTo) {
      is OperationClientMessage.Init -> writeContentsTo(writer)
      is OperationClientMessage.Start -> writeContentsTo(writer)
      is OperationClientMessage.Stop -> writeContentsTo(writer)
      is OperationClientMessage.Terminate -> writeContentsTo(writer)
    }
  }

  private fun OperationClientMessage.Init.writeContentsTo(writer: JsonWriter) {
    with(writer) {
      name(JSON_KEY_TYPE).value(OperationClientMessage.Init.TYPE)
      if (connectionParams.isNotEmpty()) {
        writer.name(JSON_KEY_PAYLOAD)
        Utils.writeToJson(connectionParams, writer)
      }
    }
  }

  private fun OperationClientMessage.Start.writeContentsTo(writer: JsonWriter) {
    with(writer) {
      name(JSON_KEY_ID).value(subscriptionId)
      name(JSON_KEY_TYPE).value(OperationClientMessage.Start.TYPE)
      name(JSON_KEY_PAYLOAD).writeObject {
        writePayloadContentsTo(writer)

        if (autoPersistSubscription) {
          name(JSON_KEY_EXTENSIONS).writeObject {
            name(JSON_KEY_EXTENSIONS_PERSISTED_QUERY).writeObject {
              name(JSON_KEY_EXTENSIONS_PERSISTED_QUERY_VERSION).value(1)
              name(JSON_KEY_EXTENSIONS_PERSISTED_QUERY_HASH).value(subscription.operationId())
            }
          }
        }
      }
    }
  }

  internal fun OperationClientMessage.Start.writePayloadContentsTo(writer: JsonWriter) {
    with(writer) {
      name(JSON_KEY_VARIABLES).jsonValue(subscription.variables().marshal(customScalarAdapters))
      name(JSON_KEY_OPERATION_NAME).value(subscription.name().name())
      if (!autoPersistSubscription || sendSubscriptionDocument) {
        name(JSON_KEY_QUERY).value(subscription.queryDocument())
      }
    }
  }

  private fun OperationClientMessage.Stop.writeContentsTo(writer: JsonWriter) {
    with(writer) {
      name(JSON_KEY_ID).value(subscriptionId)
      name(JSON_KEY_TYPE).value(OperationClientMessage.Stop.TYPE)
    }
  }

  @Suppress("unused")
  private fun OperationClientMessage.Terminate.writeContentsTo(writer: JsonWriter) {
    with(writer) {
      name(JSON_KEY_TYPE).value(OperationClientMessage.Terminate.TYPE)
    }
  }

  private fun JsonReader.readServerMessage(): OperationServerMessage {
    val responseJsonStreamReader = ResponseJsonStreamReader(this)
    val messageData = requireNotNull(responseJsonStreamReader.toMap())
    val id = messageData[OperationServerMessage.JSON_KEY_ID] as String?
    return when (val type = messageData[OperationServerMessage.JSON_KEY_TYPE] as String?) {
      OperationServerMessage.ConnectionError.TYPE -> OperationServerMessage.ConnectionError(messageData.getMessagePayload())
      OperationServerMessage.ConnectionAcknowledge.TYPE -> OperationServerMessage.ConnectionAcknowledge()
      OperationServerMessage.Data.TYPE -> OperationServerMessage.Data(id, messageData.getMessagePayload())
      OperationServerMessage.Error.TYPE -> OperationServerMessage.Error(id, messageData.getMessagePayload())
      OperationServerMessage.Complete.TYPE -> OperationServerMessage.Complete(id)
      OperationServerMessage.ConnectionKeepAlive.TYPE -> OperationServerMessage.ConnectionKeepAlive()
      else -> throw IllegalArgumentException("Unsupported message type $type")
    }
  }

  private fun Map<String, Any?>.getMessagePayload(): Map<String, Any> =
      @Suppress("UNCHECKED_CAST")
      (this[OperationServerMessage.JSON_KEY_PAYLOAD] as Map<String, Any>?)
          ?.let { Collections.unmodifiableMap(it) }
          ?: emptyMap()

  internal const val JSON_KEY_ID = "id"
  internal const val JSON_KEY_TYPE = "type"
  internal const val JSON_KEY_PAYLOAD = "payload"
  internal const val JSON_KEY_VARIABLES = "variables"
  internal const val JSON_KEY_OPERATION_NAME = "operationName"
  internal const val JSON_KEY_QUERY = "query"
  internal const val JSON_KEY_EXTENSIONS = "extensions"
  internal const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY = "persistedQuery"
  internal const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY_VERSION = "version"
  internal const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY_HASH = "sha256Hash"
}