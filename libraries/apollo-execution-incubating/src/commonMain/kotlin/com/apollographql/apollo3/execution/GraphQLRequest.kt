@file:Suppress("UNCHECKED_CAST")

package com.apollographql.apollo3.execution

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.internal.urlDecode
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.json.writeObject
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.Sink
import okio.buffer
import okio.use

class GraphQLRequest internal constructor(
    val document: String?,
    val operationName: String?,
    val variables: Map<String, Any?>,
    val extensions: Map<String, Any?>,
) : GraphQLRequestResult {
  class Builder {
    var document: String? = null
    var operationName: String? = null
    var variables: Map<String, Any?>? = null
    var extensions: Map<String, Any?>? = null

    fun document(document: String?): Builder = apply {
      this.document = document
    }

    fun operationName(operationName: String?): Builder = apply {
      this.operationName = operationName
    }

    fun variables(variables: Map<String, Any?>?): Builder = apply {
      this.variables = variables
    }

    fun extensions(extensions: Map<String, Any?>?): Builder = apply {
      this.extensions = extensions
    }

    fun build(): GraphQLRequest {
      return GraphQLRequest(
          document,
          operationName,
          variables.orEmpty(),
          extensions.orEmpty()
      )
    }
  }
}

sealed interface GraphQLRequestResult

class GraphQLRequestError internal constructor(
    val message: String,
) : GraphQLRequestResult

fun Map<String, Any?>.toGraphQLRequest(): GraphQLRequestResult {
  val map = this

  val document = map.get("query")
  if (document !is String) {
    return GraphQLRequestError("Expected 'query' to be a string")
  }

  val variables = map.get("variables")
  if (variables !is Map<*, *>?) {
    return GraphQLRequestError("Expected 'variables' to be an object")
  }

  val extensions = map.get("extensions")
  if (extensions !is Map<*, *>?) {
    return GraphQLRequestError("Expected 'extensions' to be an object")
  }

  val operationName = map.get("operationName")
  if (operationName !is String?) {
    return GraphQLRequestError("Expected 'operationName' to be a string")
  }
  return GraphQLRequest.Builder()
      .document(document)
      .variables(variables as Map<String, Any?>?)
      .extensions(extensions as Map<String, Any?>?)
      .operationName(operationName)
      .build()
}

@OptIn(ApolloInternal::class)
fun BufferedSource.parsePostGraphQLRequest(): GraphQLRequestResult {
  val map = try {
    jsonReader().use {
      it.readAny()
    }
  } catch (e: Exception) {
    return GraphQLRequestError(e.message ?: "Invalid JSON received")
  }

  if (map !is Map<*, *>) {
    return GraphQLRequestError("The received JSON is not an object")
  }

  map as Map<String, Any?>

  return map.toGraphQLRequest()
}

@OptIn(ApolloInternal::class)
fun String.parseGetGraphQLRequest(): GraphQLRequestResult {
  var fragmentStart = indexOfLast { it == '#' }
  if (fragmentStart < 0) {
    fragmentStart = length
  }
  var queryStart = fragmentStart - 1
  while (queryStart > 0) {
    if (get(queryStart) == '?') {
      break
    }
    queryStart--
  }
  // go back to after '?' (or beginning if no '?')
  queryStart++

  val query = substring(queryStart, fragmentStart)
  val pairs = query.split("&")

  val builder = GraphQLRequest.Builder()

  pairs.forEach {
    it.split("=").apply {
      if (size != 2) {
        return@forEach
      }

      when (get(0).urlDecode()) {
        "query" -> builder.document(get(1).urlDecode())
        "variables" -> {
          val variablesJson = try {
            get(1).urlDecode()
          } catch (e: Exception) {
            return GraphQLRequestError("Cannot decode 'variables' ('${get(1)}')")
          }
          val map = try {
            Buffer().writeUtf8(variablesJson).jsonReader().readAny()
          } catch (e: Exception) {
            return GraphQLRequestError("'variables' is not a valid JSON ('${variablesJson}')")
          }
          if (map !is Map<*, *>?) {
            return GraphQLRequestError("Expected 'variables' to be an object")
          }
          builder.variables(map as Map<String, Any>?)
        }

        "extensions" -> {
          val extensions = try {
            get(1).urlDecode()
          } catch (e: Exception) {
            return GraphQLRequestError("Cannot decode 'extensions' ('${get(1)}')")
          }
          val map = try {
            Buffer().writeUtf8(extensions).jsonReader().readAny()
          } catch (e: Exception) {
            return GraphQLRequestError("'extensions' is not a valid JSON ('${extensions}')")
          }
          if (map !is Map<*, *>?) {
            return GraphQLRequestError("Expected 'extensions' to be an object")
          }
          builder.extensions(map as Map<String, Any>?)
        }

        "operationName" -> builder.operationName(get(1).urlDecode())
      }
    }
  }

  return builder.build()
}

internal sealed interface ApolloWebsocketClientMessageResult

internal class ApolloWebsocketClientMessageParseError internal constructor(
    val message: String,
) : ApolloWebsocketClientMessageResult

internal sealed interface ApolloWebsocketClientMessage : ApolloWebsocketClientMessageResult

internal class ApolloWebsocketInit(
    val connectionParams: Any?,
) : ApolloWebsocketClientMessage

internal class ApolloWebsocketStart(
    val id: String,
    val request: GraphQLRequest,
) : ApolloWebsocketClientMessage

internal class ApolloWebsocketStop(
    val id: String,
) : ApolloWebsocketClientMessage


internal object ApolloWebsocketTerminate : ApolloWebsocketClientMessage

internal sealed interface ApolloWebsocketServerMessage {
  fun serialize(sink: Sink)
}

internal fun Sink.jsonWriter(): JsonWriter = BufferedSinkJsonWriter(if (this is BufferedSink) this else this.buffer())
private fun Sink.writeMessage(type: String, block: (JsonWriter.() -> Unit)? = null) {
  jsonWriter().apply {
    writeObject {
      name("type")
      value(type)
      block?.invoke(this)
    }
    flush()
  }
}

internal object ApolloWebsocketConnectionAck : ApolloWebsocketServerMessage {
  override fun serialize(sink: Sink) {
    sink.writeMessage("connection_ack")
  }
}

internal class ApolloWebsocketConnectionError(private val payload: Optional<Any?>) : ApolloWebsocketServerMessage {
  override fun serialize(sink: Sink) {
    sink.writeMessage("connection_error") {
      if (payload is Optional.Present<*>) {
        name("payload")
        writeAny(payload.value)
      }
    }
  }
}

internal class ApolloWebsocketData(
    val id: String,
    val response: GraphQLResponse,
) : ApolloWebsocketServerMessage {
  override fun serialize(sink: Sink) {
    sink.writeMessage("data") {
      name("id")
      value(id)
      name("payload")
      response.serialize(this)
    }
  }
}

internal class ApolloWebsocketError(
    val id: String?,
    val error: Error,
) : ApolloWebsocketServerMessage {
  override fun serialize(sink: Sink) {
    sink.writeMessage("error") {
      if (id != null) {
        name("id")
        value(id)
      }
      name("payload")
      writeError(error)
    }
  }
}

internal class ApolloWebsocketComplete(
    val id: String,
) : ApolloWebsocketServerMessage {
  override fun serialize(sink: Sink) {
    sink.writeMessage("complete") {
      name("id")
      value(id)
    }
  }
}

@OptIn(ApolloInternal::class)
internal fun String.parseApolloWebsocketClientMessage(): ApolloWebsocketClientMessageResult {
  val map = try {
    Buffer().writeUtf8(this).jsonReader().readAny() as Map<String, Any?>
  } catch (e: Exception) {
    return ApolloWebsocketClientMessageParseError("Malformed Json: ${e.message}")
  }

  val type = map["type"]
  if (type == null) {
    return ApolloWebsocketClientMessageParseError("No 'type' found in $this")
  }
  if (type !is String) {
    return ApolloWebsocketClientMessageParseError("'type' must be a String in $this")
  }

  when (type) {
    "start", "stop" -> {
      val id = map["id"]
      if (id == null) {
        return ApolloWebsocketClientMessageParseError("No 'id' found in $this")
      }

      if (id !is String) {
        return ApolloWebsocketClientMessageParseError("'id' must be a String in $this")
      }

      if (type == "start") {
        val payload = map["payload"]
        if (payload == null) {
          return ApolloWebsocketClientMessageParseError("No 'payload' found in $this")
        }
        if (payload !is Map<*, *>) {
          return ApolloWebsocketClientMessageParseError("'payload' must be an Object in $this")
        }

        return when (val request = (payload as Map<String, Any?>).toGraphQLRequest()) {
          is GraphQLRequestError -> ApolloWebsocketClientMessageParseError("Cannot parse start payload: '${request.message}'")
          is GraphQLRequest -> ApolloWebsocketStart(id, request = request)
        }
      } else {
        return ApolloWebsocketStop(id)
      }
    }

    "connection_init" -> {
      return ApolloWebsocketInit(map["payload"])
    }

    "connection_terminate" -> {
      return ApolloWebsocketTerminate
    }

    else -> return ApolloWebsocketClientMessageParseError("Unknown message type '$type'")
  }
}
