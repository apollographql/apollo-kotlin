package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.writeObject
import okio.Buffer
import java.io.IOException

sealed class OperationClientMessage {
  fun toJsonString(): String =
      try {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
          writer.writeObject {
            writeToJson(this)
          }
        }
        buffer.readUtf8()
      } catch (e: IOException) {
        throw RuntimeException("Failed to serialize to json", e)
      }

  @Throws(IOException::class)
  abstract fun writeToJson(writer: JsonWriter)

  companion object {
    const val JSON_KEY_ID = "id"
    const val JSON_KEY_TYPE = "type"
    const val JSON_KEY_PAYLOAD = "payload"
  }

  class Init(private val connectionParams: Map<String, Any?>) : OperationClientMessage() {
    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter) {
      writer.name(JSON_KEY_TYPE).value(TYPE)
      if (connectionParams.isNotEmpty()) {
        writer.name(JSON_KEY_PAYLOAD)
        Utils.writeToJson(connectionParams, writer)
      }
    }

    companion object {
      private const val TYPE = "connection_init"
    }
  }

  class Start(
      val subscriptionId: String,
      val subscription: Subscription<*, *, *>,
      private val scalarTypeAdapters: ScalarTypeAdapters,
      val autoPersistSubscription: Boolean,
      val sendSubscriptionDocument: Boolean
  ) : OperationClientMessage() {

    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter) {
      writer.name(JSON_KEY_ID).value(subscriptionId)
      writer.name(JSON_KEY_TYPE).value(TYPE)
      writer.name(JSON_KEY_PAYLOAD).writeObject {
        name(JSON_KEY_VARIABLES).jsonValue(subscription.variables().marshal(scalarTypeAdapters))
        name(JSON_KEY_OPERATION_NAME).value(subscription.name().name())
        if (!autoPersistSubscription || sendSubscriptionDocument) {
          writer.name(JSON_KEY_QUERY).value(subscription.queryDocument())
        }
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

    companion object {
      private const val TYPE = "start"
      private const val JSON_KEY_QUERY = "query"
      private const val JSON_KEY_VARIABLES = "variables"
      private const val JSON_KEY_OPERATION_NAME = "operationName"
      private const val JSON_KEY_EXTENSIONS = "extensions"
      private const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY = "persistedQuery"
      private const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY_VERSION = "version"
      private const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY_HASH = "sha256Hash"
    }
  }

  class Stop(val subscriptionId: String) : OperationClientMessage() {

    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter) {
      writer.name(JSON_KEY_ID).value(subscriptionId)
      writer.name(JSON_KEY_TYPE).value(TYPE)
    }

    companion object {
      private const val TYPE = "stop"
    }
  }

  class Terminate : OperationClientMessage() {
    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter) {
      writer.name(JSON_KEY_TYPE).value(TYPE)
    }

    companion object {
      private const val TYPE = "connection_terminate"
    }
  }
}