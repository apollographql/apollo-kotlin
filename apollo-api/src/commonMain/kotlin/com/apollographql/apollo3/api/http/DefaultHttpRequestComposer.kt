package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.api.AnyResponseAdapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ClientContext
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.FileUploadAwareJsonWriter
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.variablesJson
import com.benasher44.uuid.uuid4
import okio.Buffer
import okio.BufferedSink
import okio.ByteString

/**
 * The default HttpRequestComposer that handles
 * - GET or POST requests
 * - FileUpload by intercepting the Upload custom scalars and sending them as multipart if needed
 * - Automatic Persisted Queries
 * - Adding the default Apollo headers
 */
class DefaultHttpRequestComposer(val serverUrl: String, val defaultHeaders: Map<String, String> = emptyMap()) : HttpRequestComposer {

  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest {
    val params = apolloRequest.executionContext[DefaultHttpRequestComposerParams]
    val operation = apolloRequest.operation
    val responseAdapterCache = apolloRequest.executionContext[ResponseAdapterCache] ?: error("Cannot find a ResponseAdapterCache")
    val method = params?.method ?: HttpMethod.Post

    val headers = mutableMapOf(
        HEADER_APOLLO_OPERATION_ID to operation.id(),
        HEADER_APOLLO_OPERATION_NAME to operation.name()
    )

    defaultHeaders.forEach {
      headers.put(it.key, it.value)
    }
    params?.extraHeaders?.entries?.forEach {
      headers.put(it.key, it.value)
    }

    return when (method) {
      HttpMethod.Get -> {
        val url = serverUrl.appendQueryParameters(mapOf(
            "query" to operation.document(),
            "operationName" to operation.name(),
            "variables" to operation.variablesJson(responseAdapterCache)
        ))
         HttpRequest(
            method = HttpMethod.Get,
            url = url,
            headers = headers,
            body = null
        )
      }
      HttpMethod.Post -> {
        HttpRequest(
            method = HttpMethod.Post,
            url = serverUrl,
            headers = headers,
            body = getBody(apolloRequest)
        )
      }
    }
  }

  companion object {
    const val HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID"
    const val HEADER_APOLLO_OPERATION_NAME = "X-APOLLO-OPERATION-NAME"


    /**
     * A very simplified method to append query parameters
     */
    private fun String.appendQueryParameters(parameters: Map<String, String>): String = buildString {
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

    private fun <D : Operation.Data> composeOperationsJson(
        jsonWriter: JsonWriter,
        operation: Operation<D>,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
        responseAdapterCache: ResponseAdapterCache
    ) {
      jsonWriter.use { writer ->
        with(writer) {
          beginObject()
          name("operationName").value(operation.name())
          name("variables")
          beginObject()
          operation.serializeVariables(this, responseAdapterCache)
          endObject()
          if (autoPersistQueries) {
            name("extensions")
            beginObject()
            name("persistedQuery")
            beginObject()
            name("version").value(1)
            name("sha256Hash").value(operation.id())
            endObject()
            endObject()
          }
          if (!autoPersistQueries || sendDocument) {
            name("query").value(operation.document())
          }
          endObject()
        }
      }
    }

    fun <D : Operation.Data> composeOperationsJson(
        operation: Operation<D>,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
        responseAdapterCache: ResponseAdapterCache
    ): ByteString {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)

      composeOperationsJson(jsonWriter, operation, autoPersistQueries, sendDocument, responseAdapterCache)

      return buffer.readByteString()
    }

    private fun <D : Operation.Data> getBody(apolloRequest: ApolloRequest<D>): HttpBody {
      val buffer = Buffer()
      val jsonWriter = FileUploadAwareJsonWriter(BufferedSinkJsonWriter(buffer))
      val operation = apolloRequest.operation
      val params = apolloRequest.executionContext[DefaultHttpRequestComposerParams]
      val autoPersistQueries = params?.autoPersistQueries ?: false
      val sendDocument = params?.sendDocument ?: true
      val responseAdapterCache = apolloRequest.executionContext[ResponseAdapterCache] ?: error("Cannot find a ResponseAdapterCache")

      composeOperationsJson(jsonWriter, operation, autoPersistQueries, sendDocument, responseAdapterCache)

      buffer.flush()
      val operationByteString = buffer.readByteString()

      val uploads = jsonWriter.collectedUploads()
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

            val uploadsMapBuffer = uploads.toMapBuffer()
            bufferedSink.writeUtf8("\r\n--$boundary\r\n")
            bufferedSink.writeUtf8("Content-Disposition: form-data; name=\"map\"\r\n")
            bufferedSink.writeUtf8("Content-Type: application/json\r\n")
            bufferedSink.writeUtf8("Content-Length: ${uploadsMapBuffer.size}\r\n")
            bufferedSink.writeUtf8("\r\n")
            bufferedSink.writeAll(uploadsMapBuffer)

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

    private fun Map<String, Upload>.toMapBuffer(): Buffer {
      val buffer = Buffer()

      BufferedSinkJsonWriter(buffer).use {
        AnyResponseAdapter.toResponse(it, ResponseAdapterCache.DEFAULT, entries.mapIndexed { index, entry ->
          index.toString() to listOf(entry.key)
        }.toMap())
      }

      return buffer
    }
  }
}

class DefaultHttpRequestComposerParams(
    val method: HttpMethod,
    val autoPersistQueries: Boolean,
    val sendDocument: Boolean,
    val extraHeaders: Map<String, String>
) : ClientContext(Key) {
  companion object Key : ExecutionContext.Key<DefaultHttpRequestComposerParams>
}