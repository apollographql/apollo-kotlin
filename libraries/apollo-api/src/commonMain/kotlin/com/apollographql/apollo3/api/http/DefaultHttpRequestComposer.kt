package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.Upload
import com.apollographql.apollo3.api.VariablesAdapter.SerializeVariablesContext
import com.apollographql.apollo3.api.http.internal.urlEncode
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonByteString
import com.apollographql.apollo3.api.json.buildJsonMap
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.internal.FileUploadAwareJsonWriter
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.api.json.writeObject
import com.benasher44.uuid.uuid4
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import okio.Sink
import okio.blackholeSink
import okio.buffer

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
    val scalarAdapters = apolloRequest.executionContext[ScalarAdapters] ?: ScalarAdapters.Empty

    val requestHeaders = mutableListOf<HttpHeader>().apply {
      add(HttpHeader(HEADER_APOLLO_OPERATION_ID, operation.id()))
      add(HttpHeader(HEADER_APOLLO_OPERATION_NAME, operation.name()))
      if (apolloRequest.operation is Subscription<*>) {
        add(HttpHeader(HEADER_ACCEPT_NAME, HEADER_ACCEPT_VALUE_MULTIPART))
      } else {
        add(HttpHeader(HEADER_ACCEPT_NAME, HEADER_ACCEPT_VALUE_DEFER))
      }
      if (apolloRequest.httpHeaders != null) {
        addAll(apolloRequest.httpHeaders)
      }
    }

    val sendApqExtensions = apolloRequest.sendApqExtensions ?: false
    val sendDocument = apolloRequest.sendDocument ?: true

    return when (apolloRequest.httpMethod ?: HttpMethod.Post) {
      HttpMethod.Get -> {
        HttpRequest.Builder(
            method = HttpMethod.Get,
            url = buildGetUrl(serverUrl, operation, scalarAdapters, sendApqExtensions, sendDocument),
        ).addHeaders(requestHeaders)
            .build()
      }

      HttpMethod.Post -> {
        val query = if (sendDocument) operation.document() else null
        HttpRequest.Builder(
            method = HttpMethod.Post,
            url = serverUrl,
        ).addHeaders(requestHeaders)
            .body(buildPostBody(operation, scalarAdapters, sendApqExtensions, query))
            .build()
      }
    }
  }

  companion object {
    val HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID"

    // Note: in addition to this being a generally useful header to send, Apollo
    // Server's CSRF prevention feature (introduced in AS3.7 and intended to be
    // the default in AS4) includes this in the set of headers that indicate
    // that a GET request couldn't have been a non-preflighted simple request
    // and thus is safe to execute. If this project is changed to not always
    // send this header, its GET requests may be blocked by Apollo Server with
    // CSRF prevention enabled. See
    // https://www.apollographql.com/docs/apollo-server/security/cors/#preventing-cross-site-request-forgery-csrf
    // for details.
    val HEADER_APOLLO_OPERATION_NAME = "X-APOLLO-OPERATION-NAME"

    val HEADER_ACCEPT_NAME = "Accept"

    // TODO The deferSpec=20220824 part is a temporary measure so early backend implementations of the @defer directive
    // can recognize early client implementations and potentially reply in a compatible way.
    // This should be removed in later versions.
    val HEADER_ACCEPT_VALUE_DEFER = "multipart/mixed; deferSpec=20220824, application/json"
    val HEADER_ACCEPT_VALUE_MULTIPART = "multipart/mixed; boundary=\"graphql\"; subscriptionSpec=1.0, application/json"

    private fun <D : Operation.Data> buildGetUrl(
        serverUrl: String,
        operation: Operation<D>,
        scalarAdapters: ScalarAdapters,
        sendApqExtensions: Boolean,
        sendDocument: Boolean,
    ): String {
      return serverUrl.appendQueryParameters(
          composeGetParams(operation, scalarAdapters, sendApqExtensions, sendDocument)
      )
    }

    private fun <D : Operation.Data> composePostParams(
        writer: JsonWriter,
        operation: Operation<D>,
        scalarAdapters: ScalarAdapters,
        sendApqExtensions: Boolean,
        query: String?,
    ): Map<String, Upload> {
      val uploads: Map<String, Upload>
      writer.writeObject {
        name("operationName")
        value(operation.name())

        name("variables")
        val uploadAwareWriter = FileUploadAwareJsonWriter(this)
        uploadAwareWriter.writeObject {
          operation.serializeVariables(this, SerializeVariablesContext(scalarAdapters = scalarAdapters, withDefaultBooleanValues = false))
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
        scalarAdapters: ScalarAdapters,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
    ): Map<String, String> {
      val queryParams = mutableMapOf<String, String>()

      queryParams.put("operationName", operation.name())

      val variables = buildJsonString {
        val uploadAwareWriter = FileUploadAwareJsonWriter(this)
        uploadAwareWriter.writeObject {
          operation.serializeVariables(this, SerializeVariablesContext(scalarAdapters = scalarAdapters, withDefaultBooleanValues = false))
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
        scalarAdapters: ScalarAdapters,
        autoPersistQueries: Boolean,
        query: String?,
    ): HttpBody {
      val uploads: Map<String, Upload>

      val operationByteString = buildJsonByteString(indent = null) {
        uploads = composePostParams(
            this,
            operation,
            scalarAdapters,
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
        return UploadsHttpBody(uploads, operationByteString)
      }
    }

    fun <D : Operation.Data> buildParamsMap(
        operation: Operation<D>,
        scalarAdapters: ScalarAdapters,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
    ): ByteString {
      return buildJsonByteString {
        val query = if (sendDocument) operation.document() else null
        composePostParams(this, operation, scalarAdapters, autoPersistQueries, query)
      }
    }

    @Suppress("UNCHECKED_CAST")
    fun <D : Operation.Data> composePayload(
        apolloRequest: ApolloRequest<D>,
    ): Map<String, Any?> {
      val operation = apolloRequest.operation
      val sendApqExtensions = apolloRequest.sendApqExtensions ?: false
      val sendDocument = apolloRequest.sendDocument ?: true
      val scalarAdapters = apolloRequest.executionContext[ScalarAdapters] ?: error("Cannot find a ResponseAdapterCache")

      val query = if (sendDocument) operation.document() else null
      return buildJsonMap {
        composePostParams(this, operation, scalarAdapters, sendApqExtensions, query)
      } as Map<String, Any?>
    }
  }
}

@ApolloInternal
class UploadsHttpBody(
    private val uploads: Map<String, Upload>,
    private val operationByteString: ByteString,
) : HttpBody {
  private val boundary = uuid4().toString()

  override val contentType = "multipart/form-data; boundary=$boundary"

  override val contentLength by lazy {
    val countingSink = CountingSink(blackholeSink())
    val bufferedCountingSink = countingSink.buffer()
    bufferedCountingSink.writeBoundaries(writeUploadContents = false)
    bufferedCountingSink.flush()
    val result = countingSink.bytesWritten + uploads.values.sumOf { it.contentLength }
    result
  }

  override fun writeTo(bufferedSink: BufferedSink) {
    bufferedSink.writeBoundaries(writeUploadContents = true)
  }

  private fun buildUploadMap(uploads: Map<String, Upload>) = buildJsonByteString(indent = null) {
    this.writeAny(
        uploads.entries.mapIndexed { index, entry ->
          index.toString() to listOf(entry.key)
        }.toMap(),
    )
  }

  private fun BufferedSink.writeBoundaries(writeUploadContents: Boolean) {
    writeUtf8("--$boundary\r\n")
    writeUtf8("Content-Disposition: form-data; name=\"operations\"\r\n")
    writeUtf8("Content-Type: application/json\r\n")
    writeUtf8("Content-Length: ${operationByteString.size}\r\n")
    writeUtf8("\r\n")
    write(operationByteString)

    val uploadsMap = buildUploadMap(uploads)
    writeUtf8("\r\n--$boundary\r\n")
    writeUtf8("Content-Disposition: form-data; name=\"map\"\r\n")
    writeUtf8("Content-Type: application/json\r\n")
    writeUtf8("Content-Length: ${uploadsMap.size}\r\n")
    writeUtf8("\r\n")
    write(uploadsMap)

    uploads.values.forEachIndexed { index, upload ->
      writeUtf8("\r\n--$boundary\r\n")
      writeUtf8("Content-Disposition: form-data; name=\"$index\"")
      if (upload.fileName != null) {
        writeUtf8("; filename=\"${upload.fileName}\"")
      }
      writeUtf8("\r\n")
      writeUtf8("Content-Type: ${upload.contentType}\r\n")
      val contentLength = upload.contentLength
      if (contentLength != -1L) {
        writeUtf8("Content-Length: $contentLength\r\n")
      }
      writeUtf8("\r\n")
      if (writeUploadContents) {
        upload.writeTo(this)
      }
    }
    writeUtf8("\r\n--$boundary--\r\n")
  }
}

private class CountingSink(
    private val delegate: Sink,
) : Sink by delegate {
  var bytesWritten = 0L
    private set

  override fun write(source: Buffer, byteCount: Long) {
    delegate.write(source, byteCount)
    bytesWritten += byteCount
  }
}
