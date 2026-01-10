package com.apollographql.apollo.api

import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.api.json.MapJsonWriter
import com.apollographql.apollo.api.json.buildJsonByteString
import com.apollographql.apollo.api.json.internal.FileUploadAwareJsonWriter
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.api.json.writeObject
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
    val variables: Map<String, ApolloJsonElement>?,
    val extensions: Map<String, ApolloJsonElement>?,
    val uploads: Map<String, Upload>,
)

fun <D : Operation.Data> ApolloRequest<D>.toRequestParameters(extensions: Map<String, ApolloJsonElement>? = null): RequestParameters {
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

  val extensions = mutableMapOf<String, ApolloJsonElement>()
  if (sendApqExtensions) {
    extensions.put("persistedQuery", mapOf(
        "version" to 1,
        "sha256Hash" to operation.id()
    ))
  }
  if (sendEnhancedClientAwarenessExtensions) {
    extensions.put("clientLibrary", mapOf(
        "name" to "apollo-kotlin",
        "version" to apolloApiVersion
    ))
  }
  return RequestParameters(
      query = if (sendDocument) operation.document() else null,
      operationName = operation.name(),
      variables = variables,
      extensions = extensions,
      uploads = uploadAwareWriter.collectedUploads()
  )
}

fun RequestParameters.toMap(): Map<String, Any?> {
  check (uploads.isEmpty()) {
    "Apollo: sending uploads in this context is an error. Uploads are not supported by default for GET requests or subscriptions."
  }
  return toMapUnsafe()
}

fun RequestParameters.toByteString(): ByteString {
  return buildJsonByteString {
    writeAny(toMap())
  }
}

/**
 * This function doesn't check the uploads. The caller is responsible for handling uploads.
 */
internal fun RequestParameters.toMapUnsafe(): Map<String, Any?> {
  return mapOf(
      "query" to query,
      "operationName" to operationName,
      "variables" to variables,
      "extensions" to extensions,
  )
}


