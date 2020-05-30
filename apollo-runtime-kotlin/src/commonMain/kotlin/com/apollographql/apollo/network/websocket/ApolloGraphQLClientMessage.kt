package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils.writeToJson
import com.apollographql.apollo.network.GraphQLRequest
import com.benasher44.uuid.Uuid
import okio.ByteString

sealed class ApolloGraphQLClientMessage {
  abstract fun serialize(): ByteString

  class Init(private val connectionParams: Map<String, Any?>) : ApolloGraphQLClientMessage() {

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

  class Start(private val request: GraphQLRequest) : ApolloGraphQLClientMessage() {

    override fun serialize(): ByteString {
      return okio.Buffer().also { buffer ->
        JsonWriter.of(buffer)
            .beginObject()
            .name("type").value("start")
            .name("id").value(request.uuid.toString())
            .name("payload").beginObject()
            .name("variables").jsonValue(request.variables)
            .name("operationName").value(request.operationName)
            .name("query").value(request.document)
            .endObject()
            .endObject()
            .close()
      }.readByteString()
    }
  }

  class Stop(private val uuid: Uuid) : ApolloGraphQLClientMessage() {

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

  object Terminate : ApolloGraphQLClientMessage() {
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
