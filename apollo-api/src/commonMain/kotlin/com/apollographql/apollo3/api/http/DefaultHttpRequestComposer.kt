package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.http.internal.urlEncode
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.internal.FileUploadAwareJsonWriter
import com.apollographql.apollo3.api.json.buildJsonByteString
import com.apollographql.apollo3.api.json.buildJsonMap
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.json.writeObject
import com.benasher44.uuid.uuid4
import okio.BufferedSink
import okio.ByteString

/**
 * An [HttpRequestComposer] that handles:
 * - GET or POST requests
 * - FileUpload by intercepting the Upload custom scalars and sending them as multipart if needed
 * - Automatic Persisted Queries
 * - Adding the default Apollo headers
 */
class DefaultHttpRequestComposer(
    private val serverUrl: String,
) : HttpRequestComposer {

  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest {
    val operation = apolloRequest.operation
    val customScalarAdapters = apolloRequest.executionContext[CustomScalarAdapters] ?: CustomScalarAdapters.Empty

    val requestHeaders = listOf(
        HttpHeader(HEADER_APOLLO_OPERATION_ID, operation.id()),
        HttpHeader(HEADER_APOLLO_OPERATION_NAME, operation.name())
    ) + (apolloRequest.httpHeaders ?: emptyList())

    val sendApqExtensions = apolloRequest.sendApqExtensions ?: false
    val sendDocument = apolloRequest.sendDocument ?: true

    return when (apolloRequest.httpMethod ?: HttpMethod.Post) {
      HttpMethod.Get -> {
        HttpRequest.Builder(
            method = HttpMethod.Get,
            url = buildGetUrl(serverUrl, operation, customScalarAdapters, sendApqExtensions, sendDocument),
        ).addHeaders(requestHeaders)
            .build()
      }
      HttpMethod.Post -> {
        val query = if (sendDocument) operation.document() else null
        HttpRequest.Builder(
            method = HttpMethod.Post,
            url = serverUrl,
        ).addHeaders(requestHeaders)
            .body(buildPostBody(operation, customScalarAdapters, sendApqExtensions, query))
            .build()
      }
    }
  }

  companion object {
    const val HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID"
    const val HEADER_APOLLO_OPERATION_NAME = "X-APOLLO-OPERATION-NAME"

    private fun <D : Operation.Data> buildGetUrl(
        serverUrl: String,
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        sendApqExtensions: Boolean,
        sendDocument: Boolean,
    ): String {
      return serverUrl.appendQueryParameters(
          composeGetParams(operation, customScalarAdapters, sendApqExtensions, sendDocument)
      )
    }

    private fun <D : Operation.Data> composePostParams(
        writer: JsonWriter,
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        sendApqExtensions: Boolean,
        query: String?,
    ): Map<String, Upload> {
      val uploads: Map<String, Upload>
      @OptIn(ApolloInternal::class)
      writer.writeObject {
        name("operationName")
        value(operation.name())

        name("variables")
        val uploadAwareWriter = FileUploadAwareJsonWriter(this)
        uploadAwareWriter.writeObject {
          operation.serializeVariables(this, customScalarAdapters)
        }
        uploads = uploadAwareWriter.collectedUploads()

        if (query != null) {
          name("query")
          value(query)
        }

        if (sendApqExtensions) {
          name("extensions")
          writeObject {
            name("persistedQuery")
            writeObject {
              name("version").value(1)
              name("sha256Hash").value(operation.id())
            }
          }
        }
      }

      return uploads
    }

    /**
     * This mostly duplicates [composePostParams] but encode variables and extensions as strings
     * and not json elements. I tried factoring in that code but it ended up being more clunky that
     * duplicating it
     */
    private fun <D : Operation.Data> composeGetParams(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
    ): Map<String, String> {
      val queryParams = mutableMapOf<String, String>()

      queryParams.put("operationName", operation.name())

      @OptIn(ApolloInternal::class)
      val variables = buildJsonString {
        val uploadAwareWriter = FileUploadAwareJsonWriter(this)
        uploadAwareWriter.writeObject {
          operation.serializeVariables(this, customScalarAdapters)
        }
        check(uploadAwareWriter.collectedUploads().isEmpty()) {
          "FileUpload and Http GET are not supported at the same time"
        }
      }

      queryParams.put("variables", variables)

      if (sendDocument) {
        queryParams.put("query", operation.document())
      }

      if (autoPersistQueries) {
        @OptIn(ApolloInternal::class)
        val extensions = buildJsonString {
          writeObject {
            name("persistedQuery")
            writeObject {
              name("version").value(1)
              name("sha256Hash").value(operation.id())
            }
          }
        }
        queryParams.put("extensions", extensions)
      }
      return queryParams
    }

    /**
     * A very simplified method to append query parameters
     */
    fun String.appendQueryParameters(parameters: Map<String, String>): String = buildString {
      append(this@appendQueryParameters)
      var hasQuestionMark = this@appendQueryParameters.contains("?")

      parameters.entries.forEach {
        if (hasQuestionMark) {
          append('&')
        } else {
          hasQuestionMark = true
          append('?')
        }
        append(it.key.urlEncode())
        append('=')
        append(it.value.urlEncode())
      }
    }

    fun <D : Operation.Data> buildPostBody(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        autoPersistQueries: Boolean,
        query: String?,
    ): HttpBody {
      val uploads: Map<String, Upload>

      @OptIn(ApolloInternal::class)
      val operationByteString = buildJsonByteString(indent = null) {
        uploads = composePostParams(
            this,
            operation,
            customScalarAdapters,
            autoPersistQueries,
            query
        )
      }

      if (uploads.isEmpty()) {
        return object : HttpBody {
          override val contentType = "application/json"
          override val contentLength = operationByteString.size.toLong()

          override fun writeTo(bufferedSink: BufferedSink) {
            bufferedSink.write(operationByteString)
          }
        }
      } else {
        return object : HttpBody {
          private val boundary = uuid4().toString()

          override val contentType = "multipart/form-data; boundary=$boundary"

          // XXX: support non-chunked multipart
          override val contentLength = -1L

          override fun writeTo(bufferedSink: BufferedSink) {
            bufferedSink.writeUtf8("--$boundary\r\n")
            bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"operations\"\r\n")
            bufferedSink.writeUtf8("Content-Type: application/json\r\n")
            bufferedSink.writeUtf8("Content-Length: ${operationByteString.size}\r\n")
            bufferedSink.writeUtf8("\r\n")
            bufferedSink.write(operationByteString)

            val uploadsMap = buildUploadMap(uploads)
            bufferedSink.writeUtf8("\r\n--$boundary\r\n")
            bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"map\"\r\n")
            bufferedSink.writeUtf8("Content-Type: application/json\r\n")
            bufferedSink.writeUtf8("Content-Length: ${uploadsMap.size}\r\n")
            bufferedSink.writeUtf8("\r\n")
            bufferedSink.write(uploadsMap)

            uploads.values.forEachIndexed { index, upload ->
              bufferedSink.writeUtf8("\r\n--$boundary\r\n")
              bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"$index\"")
              if (upload.fileName != null) {
                bufferedSink.writeUtf8("; filename=\"${upload.fileName}\"")
              }
              bufferedSink.writeUtf8("\r\n")
              bufferedSink.writeUtf8("Content-Type: ${upload.contentType}\r\n")
              val contentLength = upload.contentLength
              if (contentLength != -1L) {
                bufferedSink.writeUtf8("Content-Length: $contentLength\r\n")
              }
              bufferedSink.writeUtf8("\r\n")
              upload.writeTo(bufferedSink)
            }
            bufferedSink.writeUtf8("\r\n--$boundary--\r\n")
          }
        }
      }
    }

    @OptIn(ApolloInternal::class)
    private fun buildUploadMap(uploads: Map<String, Upload>) = buildJsonByteString(indent = null) {
      this.writeAny(
          uploads.entries.mapIndexed { index, entry ->
            index.toString() to listOf(entry.key)
          }.toMap(),
      )
    }


    fun <D : Operation.Data> buildParamsMap(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
    ): ByteString {
      @OptIn(ApolloInternal::class)
      return buildJsonByteString {
        val query = if (sendDocument) operation.document() else null
        composePostParams(this, operation, customScalarAdapters, autoPersistQueries, query)
      }
    }

    @Suppress("UNCHECKED_CAST")
    fun <D : Operation.Data> composePayload(
        apolloRequest: ApolloRequest<D>,
    ): Map<String, Any?> {
      val operation = apolloRequest.operation
      val sendApqExtensions = apolloRequest.sendApqExtensions ?: false
      val sendDocument = apolloRequest.sendDocument ?: true
      val customScalarAdapters = apolloRequest.executionContext[CustomScalarAdapters] ?: error("Cannot find a ResponseAdapterCache")

      val query = if (sendDocument) operation.document() else null
      @OptIn(ApolloInternal::class)
      return buildJsonMap {
        composePostParams(this, operation, customScalarAdapters, sendApqExtensions, query)
      } as Map<String, Any?>
    }
  }
}
