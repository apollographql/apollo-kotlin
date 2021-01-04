package com.apollographql.apollo.network.ws

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils.writeToJson
import com.apollographql.apollo.interceptor.ApolloRequest
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
            .name("payload").apply { writeToJson(connectionParams, this) }
            .endObject()
            .close()
      }.readByteString()
    }
  }

  @ApolloExperimental
  class Start(private val request: ApolloRequest<*>) : ApolloGraphQLClientMessage() {

    override fun serialize(): ByteString {
      return okio.Buffer().also { buffer ->
        JsonWriter.of(buffer)
            .beginObject()
            .name("type").value("start")
            .name("id").value(request.requestUuid.toString())
            .name("payload").jsonValue(request.operation.composeRequestBody(request.customScalarAdapters).utf8())
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
