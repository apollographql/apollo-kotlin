package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils.writeToJson
import com.benasher44.uuid.Uuid
import okio.ByteString

sealed class GraphQLClientMessage {
  abstract fun serialize(): ByteString

  class Init(private val connectionParams: Map<String, Any?>) : GraphQLClientMessage() {

    override fun serialize(): ByteString {
      return okio.Buffer().also { buffer ->
        JsonWriter.of(buffer)
            .beginObject()
            .name("type").value("connection_init")
            .apply {
              if (connectionParams.isNotEmpty()) {
                name("payload")
                writeToJson(connectionParams, this)
              }
            }
            .endObject()
            .close()
      }.readByteString()
    }
  }

  class Start(
      private val uuid: Uuid,
      private val operation: Operation<*, *, *>,
      private val scalarTypeAdapters: ScalarTypeAdapters
  ) : GraphQLClientMessage() {

    override fun serialize(): ByteString {
      return okio.Buffer().also { buffer ->
        JsonWriter.of(buffer)
            .beginObject()
            .name("type").value("start")
            .name("id").value(uuid.toString())
            .name("payload").beginObject()
            .name("variables").jsonValue(operation.variables().marshal(scalarTypeAdapters))
            .name("operationName").value(operation.name().name())
            .name("query").value(operation.queryDocument())
            .endObject()
            .close()
      }.readByteString()
    }
  }

  class Stop(private val uuid: Uuid) : GraphQLClientMessage() {

    override fun serialize(): ByteString {
      return okio.Buffer().also { buffer ->
        JsonWriter.of(buffer)
            .beginObject()
            .name("type").value("stop")
            .name("id").value(uuid.toString())
            .endObject()
            .close()
      }.readByteString()
    }
  }

  object Terminate : GraphQLClientMessage() {
    override fun serialize(): ByteString {
      return okio.Buffer().also { buffer ->
        JsonWriter.of(buffer)
            .beginObject()
            .name("type").value("connection_terminate")
            .endObject()
            .close()
      }.readByteString()
    }
  }
}
