package com.apollographql.apollo.api

import com.apollographql.apollo.api.http.HttpBody
import com.apollographql.apollo.api.http.UploadsHttpBody
import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.api.json.MapJsonWriter
import com.apollographql.apollo.api.json.buildJsonByteString
import com.apollographql.apollo.api.json.internal.FileUploadAwareJsonWriter
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.api.json.writeObject
import okio.BufferedSink
import okio.ByteString

/**
 * The request parameters.
 *
 * See https://graphql.github.io/graphql-over-http/draft/#sec-Request-Parameters
 *
 * @property query the GraphQL executable document to execute. May be null for persisted queries.
 * @property operationName the name of the operation to execute.
 * @property variables the variables to use for the query.
 * @property extensions additional parameters for the request.
 * @property uploads files to upload.
 */
class RequestParameters(
    val query: String?,
    val operationName: String,
    val variables: Map<String, ApolloJsonElement>,
    val extensions: Map<String, ApolloJsonElement>,
    val uploads: Map<String, Upload>,
)

fun <D : Operation.Data> ApolloRequest<D>.toRequestParameters(): RequestParameters {
  val sendApqExtensions = sendApqExtensions ?: false
  val sendEnhancedClientAwarenessExtensions = sendEnhancedClientAwareness
  val sendDocument = sendDocument ?: true
  val scalarAdapters = executionContext[CustomScalarAdapters]!!

  val jsonWriter = MapJsonWriter()
  val uploadAwareWriter = FileUploadAwareJsonWriter(jsonWriter)
  uploadAwareWriter.writeObject {
    operation.serializeVariables(this, scalarAdapters, false)
  }

  @Suppress("UNCHECKED_CAST")
  val variables = jsonWriter.root() as Map<String, ApolloJsonElement>

  val ext = mutableMapOf<String, ApolloJsonElement>()
  if (sendApqExtensions) {
    ext.put("persistedQuery", mapOf(
        "version" to 1,
        "sha256Hash" to operation.id()
    ))
  }
  if (sendEnhancedClientAwarenessExtensions) {
    ext.put("clientLibrary", mapOf(
        "name" to "apollo-kotlin",
        "version" to apolloApiVersion
    ))
  }
  if (extensions != null) {
    ext.putAll(extensions)
  }
  return RequestParameters(
      query = if (sendDocument) operation.document() else null,
      operationName = operation.name(),
      variables = variables,
      extensions = ext,
      uploads = uploadAwareWriter.collectedUploads()
  )
}

/**
 * Turns the [RequestParameters] into a [Map]
 *
 * @throws IllegalStateException if the [RequestParameters] contain uploads.
 */
fun RequestParameters.toMap(): Map<String, Any?> {
  check(uploads.isEmpty()) {
    "Apollo: sending uploads in this context is an error. Uploads are not supported by default for GET requests or subscriptions."
  }
  return toMapUnsafe()
}

/**
 * Turns the [RequestParameters] into a [ByteString]
 *
 * @throws IllegalStateException if the [RequestParameters] contain uploads.
 */
fun RequestParameters.toByteString(): ByteString {
  return buildJsonByteString {
    writeAny(toMap())
  }
}

/**
 * This function doesn't check the uploads. The caller is responsible for handling uploads.
 */
internal fun RequestParameters.toMapUnsafe(): Map<String, Any?> {
  return buildMap {
    if (query != null) {
      put("query", query)
    }
    put("operationName", operationName)
    if (variables.isNotEmpty()) {
      put("variables", variables)
    }
    if (extensions.isNotEmpty()) {
      put("extensions", extensions)
    }
  }
}

/**
 * Turns the [RequestParameters] into a [HttpBody]
 *
 * The [HttpBody] may be a multipart body if uploads are present.
 */
fun RequestParameters.toHttpBody(): HttpBody {
  // Do not replace this with `toByteString()`: there may be uploads
  val byteString = buildJsonByteString {
    writeAny(toMapUnsafe())
  }

  return if (uploads.isEmpty()) {
    object : HttpBody {
      override val contentType = "application/json"
      override val contentLength = byteString.size.toLong()

      override fun writeTo(bufferedSink: BufferedSink) {
        bufferedSink.write(byteString)
      }
    }
  } else {
    UploadsHttpBody(uploads, byteString)
  }
}