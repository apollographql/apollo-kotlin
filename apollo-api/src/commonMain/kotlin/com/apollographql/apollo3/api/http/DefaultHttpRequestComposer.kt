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
import com.apollographql.apollo3.api.internal.json.buildJsonByteString
import com.apollographql.apollo3.api.internal.json.buildJsonString
import com.apollographql.apollo3.api.internal.json.writeObject
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.variablesJson
import com.benasher44.uuid.uuid4
import okio.Buffer
import okio.BufferedSink

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
    val method = params?.method ?: HttpMethod.Post
    val autoPersistQueries = params?.autoPersistQueries ?: false
    val sendDocument = params?.sendDocument ?: true
    val responseAdapterCache = apolloRequest.executionContext[ResponseAdapterCache] ?: error("Cannot find a ResponseAdapterCache")

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

    val paramsMap = mutableMapOf<String, String>()
    val (variables, uploads) = buildVariables(operation, responseAdapterCache)

    paramsMap.put("operationName", operation.name())
    paramsMap.put("variables", variables)
    if (sendDocument) {
      paramsMap.put("query", operation.document())
    }
    if (autoPersistQueries) {
      paramsMap.put("extensions", buildExtensions(operation.id()))
    }

    return when (method) {
      HttpMethod.Get -> {
        check(uploads.isEmpty()) {
          "GET is not supported with File Upload"
        }
        HttpRequest(
            method = HttpMethod.Get,
            url = serverUrl.appendQueryParameters(paramsMap),
            headers = headers,
            body = null
        )
      }
      HttpMethod.Post -> {
        HttpRequest(
            method = HttpMethod.Post,
            url = serverUrl,
            headers = headers,
            body = buildBody(paramsMap, uploads)
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


    private data class BuiltVariables(val variables: String, val uploads: Map<String, Upload>)

    private fun <D : Operation.Data> buildVariables(operation: Operation<D>, responseAdapterCache: ResponseAdapterCache): BuiltVariables {
      val buffer = Buffer()
      val jsonWriter = FileUploadAwareJsonWriter(BufferedSinkJsonWriter(buffer))
      jsonWriter.writeObject {
        operation.serializeVariables(this, responseAdapterCache)
      }
      return BuiltVariables(buffer.readUtf8(), jsonWriter.collectedUploads())
    }

    private fun buildBody(
        paramsMap: Map<String, String>,
        uploads: Map<String, Upload>,
    ): HttpBody {
      val operationByteString = buildJsonByteString {
        AnyResponseAdapter.toResponse(this, paramsMap)
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


    private fun buildExtensions(
        operationId: String,
    ): String {
      return buildJsonString {
        writeObject {
          name("persistedQuery")
          writeObject {
            name("version").value(1)
            name("sha256Hash").value(operationId)
          }
        }
      }
    }

    private fun buildUploadMap(uploads: Map<String, Upload>) = buildJsonByteString {
      AnyResponseAdapter.toResponse(this, ResponseAdapterCache.DEFAULT, uploads.entries.mapIndexed { index, entry ->
        index.toString() to listOf(entry.key)
      }.toMap())
    }
  }
}

data class DefaultHttpRequestComposerParams(
    val method: HttpMethod,
    val autoPersistQueries: Boolean,
    val sendDocument: Boolean,
    val extraHeaders: Map<String, String>,
) : ClientContext(Key) {
  companion object Key : ExecutionContext.Key<DefaultHttpRequestComposerParams>
}