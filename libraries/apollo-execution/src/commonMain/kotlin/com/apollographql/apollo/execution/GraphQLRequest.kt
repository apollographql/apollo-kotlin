@file:Suppress("UNCHECKED_CAST")

package com.apollographql.apollo.execution

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.http.internal.urlDecode
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.execution.internal.flatMap
import okio.Buffer
import okio.BufferedSource
import okio.use

enum class OnError {
  NULL,
  PROPAGATE,
  HALT
}

/**
 * @property document the document. Can be null if persisted queries are used.
 * @property operationName the name of the operation to execute (optional). Useful if [document] contains several operations.
 * @property variables the variables.
 * @property extensions the extensions.
 */
class GraphQLRequest internal constructor(
    val document: String?,
    val operationName: String?,
    val variables: Map<String, ExternalValue>,
    val extensions: Map<String, ExternalValue>,
    val onError: OnError?,
) {
  class Builder {
    var document: String? = null
    var operationName: String? = null
    var variables: Map<String, ExternalValue>? = null
    var extensions: Map<String, ExternalValue>? = null

    var onError: OnError? = null

    fun document(document: String?): Builder = apply {
      this.document = document
    }

    fun operationName(operationName: String?): Builder = apply {
      this.operationName = operationName
    }

    fun variables(variables: Map<String, ExternalValue>?): Builder = apply {
      this.variables = variables
    }

    fun extensions(extensions: Map<String, ExternalValue>?): Builder = apply {
      this.extensions = extensions
    }

    fun onError(onError: OnError?): Builder = apply {
      this.onError = onError
    }

    fun build(): GraphQLRequest {
      return GraphQLRequest(
          document = document,
          operationName = operationName,
          variables = variables.orEmpty(),
          extensions = extensions.orEmpty(),
          onError = onError
      )
    }
  }
}

/**
 * Parses a map of external values to a [GraphQLRequest].
 *
 * Note: this is typically used by subscriptions and/or post requests. GET request encode "variables" and "extensions" as JSON and
 * need to be preprocessed first. See [toExternalValueMap].
 */
fun Map<String, ExternalValue>.parseAsGraphQLRequest(): Result<GraphQLRequest> {
  val map = this

  val document = map.get("query")
  if (document !is String?) {
    return Result.failure(Exception("Expected 'query' to be a string"))
  }

  val variables = map.get("variables")
  if (variables !is Map<*, *>?) {
    return Result.failure(Exception("Expected 'variables' to be an object"))
  }

  val extensions = map.get("extensions")
  if (extensions !is Map<*, *>?) {
    return Result.failure(Exception("Expected 'extensions' to be an object"))
  }

  val operationName = map.get("operationName")
  if (operationName !is String?) {
    return Result.failure(Exception("Expected 'operationName' to be a string"))
  }
  val onError = map.get("onError")
  var onErrorValue: OnError? = null
  if (onError != null) {
    if (onError !is String) {
      return Result.failure(Exception("Expected 'onError' to be a string"))
    }
    onErrorValue = when (onError) {
      "NULL" -> OnError.PROPAGATE
      "PROPAGATE" -> OnError.PROPAGATE
      "HALT" -> OnError.HALT
      else -> return Result.failure(Exception("Unknown 'onError' value: $onError"))
    }
  }

  return GraphQLRequest.Builder()
      .document(document)
      .variables(variables as Map<String, Any?>?)
      .extensions(extensions as Map<String, Any?>?)
      .operationName(operationName)
      .onError(onErrorValue)
      .build().let {
        Result.success(it)
      }
}

@OptIn(ApolloInternal::class)
fun BufferedSource.parseAsGraphQLRequest(): Result<GraphQLRequest> {
  val map = try {
    jsonReader().use {
      it.readAny()
    }
  } catch (e: Exception) {
    return Result.failure(e)
  }

  if (map !is Map<*, *>) {
    return Result.failure(Exception("The received JSON is not an object"))
  }

  map as Map<String, ExternalValue>

  return map.parseAsGraphQLRequest()
}

/**
 * Parses the given query string to a GraphQL request
 *
 * @receiver an HTTP query string like `"key1=value1&key2=value2"`
 */
fun String.parseAsGraphQLRequest(): Result<GraphQLRequest> {
  val pairs = split("&")

  return pairs.filter { it.isNotBlank() }.map { pair ->
    pair.split("=").let {
      if (it.size != 2) {
        return Result.failure(Exception("Invalid query parameter '$pair'"))
      }
      it.get(0).urlDecode() to it.get(1).urlDecode()
    }
  }.groupBy { it.first }
      .mapValues {
        it.value.map { it.second }
      }
      .toExternalValueMap()
      .flatMap {
        it.parseAsGraphQLRequest()
      }
}

/**
 * Converts a multi-map such as one from parsed query params to a map of external values.
 */
fun Map<String, List<String>>.toExternalValueMap(): Result<Map<String, ExternalValue>> {
  val map = mapValues {
    when (it.key) {
      "query" -> {
        if (it.value.size != 1) {
          return Result.failure(Exception("multiple 'query' parameter found"))
        }

        it.value.first()
      }

      "variables" -> {
        if (it.value.size != 1) {
          return Result.failure(Exception("multiple 'variables' parameter found"))
        }

        try {
          it.value.first().readAny()
        } catch (e: Exception) {
          return Result.failure(e)
        }
      }

      "extensions" -> {
        if (it.value.size != 1) {
          return Result.failure(Exception("multiple 'extensions' parameter found"))
        }

        try {
          it.value.first().readAny()
        } catch (e: Exception) {
          return Result.failure(e)
        }
      }

      "operationName" -> {
        if (it.value.size != 1) {
          return Result.failure(Exception("multiple 'operationName' parameter found"))
        }

        it.value.first()
      }

      else -> it.value
    }
  }

  return Result.success(map)
}

@OptIn(ApolloInternal::class)
private fun String.readAny(): Any? = Buffer().writeUtf8(this).jsonReader().readAny()