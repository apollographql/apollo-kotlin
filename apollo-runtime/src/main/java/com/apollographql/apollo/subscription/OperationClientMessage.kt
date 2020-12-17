package com.apollographql.apollo.subscription

import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.writeObject
import okio.Buffer
import java.io.IOException

sealed class OperationClientMessage {
  @JvmOverloads
  fun toJsonString(
      writePayloadAsJsonString: Boolean = false,
      extensions: Map<String, Any?> = emptyMap()
  ): String =
      try {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
          writer.writeObject {
            writeToJson(this, writePayloadAsJsonString, extensions)
          }
        }
        buffer.readUtf8()
      } catch (e: IOException) {
        throw RuntimeException("Failed to serialize to json", e)
      }

  @Throws(IOException::class)
  fun writeToJson(writer: JsonWriter) = writeToJson(writer, false, emptyMap())
  fun writeToJson(writer: JsonWriter, writePayloadAsJsonString: Boolean) = writeToJson(writer, writePayloadAsJsonString, emptyMap())

  @Throws(IOException::class)
  abstract fun writeToJson(
      writer: JsonWriter,
      writePayloadAsJsonString: Boolean = false,
      extensions: Map<String, Any?> = emptyMap()
  )

  companion object {
    private const val JSON_KEY_ID = "id"
    private const val JSON_KEY_TYPE = "type"
    private const val JSON_KEY_PAYLOAD = "payload"
  }

  class Init(private val connectionParams: Map<String, Any?>) : OperationClientMessage() {
    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter, writePayloadAsJsonString: Boolean, extensions: Map<String, Any?>) {
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
      @JvmField
      val subscriptionId: String,
      @JvmField
      val subscription: Subscription<*, *, *>,
      private val scalarTypeAdapters: ScalarTypeAdapters,
      @JvmField
      val autoPersistSubscription: Boolean,
      @JvmField
      val sendSubscriptionDocument: Boolean
  ) : OperationClientMessage() {

    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter, writePayloadAsJsonString: Boolean, extensions: Map<String, Any?>) {
      require(JSON_KEY_EXTENSIONS_PERSISTED_QUERY !in extensions) {
        "The extensions must not contain $JSON_KEY_EXTENSIONS_PERSISTED_QUERY"
      }

      writer.name(JSON_KEY_ID).value(subscriptionId)
      writer.name(JSON_KEY_TYPE).value(TYPE)
      writer.name(JSON_KEY_PAYLOAD).writeObject {
        if (writePayloadAsJsonString) {
          val buffer = Buffer()
          JsonWriter.of(buffer).writeObject { writePayload() }
          name(JSON_KEY_DATA).value(buffer.readUtf8())
        } else {
          writePayload()
        }

        if (autoPersistSubscription || extensions.isNotEmpty()) {
          name(JSON_KEY_EXTENSIONS).writeObject {
            if (autoPersistSubscription) {
              name(JSON_KEY_EXTENSIONS_PERSISTED_QUERY).writeObject {
                name(JSON_KEY_EXTENSIONS_PERSISTED_QUERY_VERSION).value(1)
                name(JSON_KEY_EXTENSIONS_PERSISTED_QUERY_HASH).value(subscription.operationId())
              }
            }
            for ((name, value) in extensions) {
              Utils.writeToJson(value, name(name))
            }
          }
        }
      }
    }

    private fun JsonWriter.writePayload() {
      name(JSON_KEY_VARIABLES).jsonValue(subscription.variables().marshal(scalarTypeAdapters))
      name(JSON_KEY_OPERATION_NAME).value(subscription.name().name())
      if (!autoPersistSubscription || sendSubscriptionDocument) {
        name(JSON_KEY_QUERY).value(subscription.queryDocument())
      }
    }

    companion object {
      private const val TYPE = "start"
      private const val JSON_KEY_DATA = "data"
      private const val JSON_KEY_QUERY = "query"
      private const val JSON_KEY_VARIABLES = "variables"
      private const val JSON_KEY_OPERATION_NAME = "operationName"
      private const val JSON_KEY_EXTENSIONS = "extensions"
      private const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY = "persistedQuery"
      private const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY_VERSION = "version"
      private const val JSON_KEY_EXTENSIONS_PERSISTED_QUERY_HASH = "sha256Hash"
    }
  }

  class Stop(@JvmField val subscriptionId: String) : OperationClientMessage() {

    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter, writePayloadAsJsonString: Boolean, extensions: Map<String, Any?>) {
      writer.name(JSON_KEY_ID).value(subscriptionId)
      writer.name(JSON_KEY_TYPE).value(TYPE)
    }

    companion object {
      private const val TYPE = "stop"
    }
  }

  class Terminate : OperationClientMessage() {
    @Throws(IOException::class)
    override fun writeToJson(writer: JsonWriter, writePayloadAsJsonString: Boolean, extensions: Map<String, Any?>) {
      writer.name(JSON_KEY_TYPE).value(TYPE)
    }

    companion object {
      private const val TYPE = "connection_terminate"
    }
  }
}