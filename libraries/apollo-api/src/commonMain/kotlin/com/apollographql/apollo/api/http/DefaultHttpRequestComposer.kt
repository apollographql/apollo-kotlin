package com.apollographql.apollo.api.http

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.Upload
import com.apollographql.apollo.api.http.internal.urlEncode
import com.apollographql.apollo.api.json.ApolloJsonElement
import com.apollographql.apollo.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.buildJsonByteString
import com.apollographql.apollo.api.json.buildJsonMap
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.api.toByteString
import com.apollographql.apollo.api.toHttpBody
import com.apollographql.apollo.api.toMap
import com.apollographql.apollo.api.toRequestParameters
import com.benasher44.uuid.uuid4
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import okio.HashingSink
import okio.Sink
import okio.blackholeSink
import okio.buffer

/**
 * An [HttpRequestComposer] that handles:
 * - GET or POST requests
 * - FileUpload by intercepting the Upload custom scalars and sending them as multipart if needed
 * - Automatic Persisted Queries
 * - Adding the default Apollo headers
 *
 * @param enablePostCaching enables caching of query POST requests using [CacheUrlOverride]
 */
class DefaultHttpRequestComposer(
    private val serverUrl: String?,
    private val enablePostCaching: Boolean,
) : HttpRequestComposer {

  constructor(serverUrl: String?) : this(serverUrl, false)

  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): HttpRequest {
    val requestHeaders = mutableListOf<HttpHeader>().apply {
      if (apolloRequest.httpHeaders != null) {
        addAll(apolloRequest.httpHeaders)
      }
      if (get("accept") == null) {
        /** 
          * This is for backward compatibility reasons only. 
          * We should encourage users to set the accept headers before calling DefaultHttpRequestComposer         
          */
        add(
            HttpHeader("accept",
                if (apolloRequest.operation is Subscription<*>) {
                  "multipart/mixed;subscriptionSpec=1.0, application/graphql-response+json, application/json"
                } else {
                  "application/graphql-response+json, application/json"
                }
            )
        )
      }
    }

    val requestParameters = apolloRequest.toRequestParameters()

    val url = apolloRequest.url ?: serverUrl ?: error("ApolloRequest.url is missing for request '${apolloRequest.operation.name()}', did you call ApolloClient.Builder.serverUrl(url)?")
    val httpRequestBuilder = when (apolloRequest.httpMethod ?: HttpMethod.Post) {
      HttpMethod.Get -> {

        @Suppress("DEPRECATION")
        HttpRequest.Builder(
            method = HttpMethod.Get,
            url = url.appendQueryParameters(requestParameters.toMap().asGetParameters()),
        ).addHeader(HEADER_APOLLO_REQUIRE_PREFLIGHT, "true")
      }

      HttpMethod.Post -> {
        val body = requestParameters.toHttpBody()
        HttpRequest.Builder(
            method = HttpMethod.Post,
            url = url,
        ).apply {
          body(body)
          if (body.contentType.startsWith("multipart/form-data")) {
            addHeader(HEADER_APOLLO_REQUIRE_PREFLIGHT, "true")
          }
          val operation = apolloRequest.operation
          if (enablePostCaching && operation is Query<*>) {
            val cacheParameters = mutableMapOf<String, String>()

            if (requestParameters.variables.isNotEmpty()) {
              cacheParameters.put("variablesHash", requestParameters.variables.sha256())
            }
            cacheParameters.put("operationName", operation.name())
            cacheParameters.put("operationId", operation.id())

            @Suppress("DEPRECATION")
            addExecutionContext(CacheUrlOverride(url.appendQueryParameters(cacheParameters)))
          }
        }
      }
    }

    return httpRequestBuilder
        .addHeaders(requestHeaders)
        .addExecutionContext(apolloRequest.executionContext)
        .build()
  }

  private fun Any.sha256(): String {
    val hashingSink = HashingSink.sha256(blackholeSink())
    val buffer = hashingSink.buffer()
    BufferedSinkJsonWriter(buffer).writeAny(this)
    buffer.flush()
    return hashingSink.hash.hex()
  }

  companion object {
    // Note: Apollo Server's CSRF prevention feature (introduced in AS3.7 and intended to be
    // the default in AS4) includes this in the set of headers that indicate
    // that a GET request couldn't have been a non-preflighted simple request
    // and thus is safe to execute.
    // See https://www.apollographql.com/docs/apollo-server/security/cors/#preventing-cross-site-request-forgery-csrf
    // for details.
    private const val HEADER_APOLLO_REQUIRE_PREFLIGHT = "Apollo-Require-Preflight"

    @Deprecated("This was made public by mistake and will be removed in a future version, please use your own constants instead")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    val HEADER_ACCEPT_NAME = "Accept"

    @Deprecated("This was made public by mistake and will be removed in a future version, please use your own constants instead")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    val HEADER_ACCEPT_VALUE_DEFER = "multipart/mixed;deferSpec=20220824, application/graphql-response+json, application/json"

    @Deprecated("This was made public by mistake and will be removed in a future version, please use your own constants instead")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    val HEADER_ACCEPT_VALUE_MULTIPART = "multipart/mixed;subscriptionSpec=1.0, application/graphql-response+json, application/json"

    /**
     * A very simplified method to append query parameters
     */
    @Deprecated("appendQueryParameters was not supposed to be exposed and will be removed in a future version. Use a dedicated URL parsing library instead.")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
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

    @Deprecated("Use buildPostBody(operation, customScalarAdapters, query, extensionsWriter) instead", level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun <D : Operation.Data> buildPostBody(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        autoPersistQueries: Boolean,
        sendEnhancedClientAwarenessExtensions: Boolean,
        query: String?,
    ): HttpBody {
      if (query != null) {
        require(query == operation.document()) {
          "The query parameter must match the operation document"
        }
      }

      return ApolloRequest.Builder(operation)
          .addExecutionContext(customScalarAdapters)
          .sendDocument(if (query == null) false else true)
          .sendApqExtensions(autoPersistQueries)
          .sendEnhancedClientAwareness(sendEnhancedClientAwarenessExtensions)
          .build()
          .toRequestParameters()
          .toHttpBody()

    }

    @Deprecated("Use `toRequestParameters()` instead.", ReplaceWith("apolloRequest.toRequestParameters().toHttpBody()"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_3_1)
    fun <D : Operation.Data> buildPostBody(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        query: String?,
        extensionsWriter: JsonWriter.() -> Unit,
    ): HttpBody {
      if (query != null) {
        require(query == operation.document()) {
          "The query parameter must match the operation document"
        }
      }

      @Suppress("UNCHECKED_CAST")
      return ApolloRequest.Builder(operation)
          .addExecutionContext(customScalarAdapters)
          .sendDocument(if (query == null) false else true)
          .extensions(buildJsonMap {
            extensionsWriter()
          } as Map<String, ApolloJsonElement>)
          .build()
          .toRequestParameters()
          .toHttpBody()
    }

    @Deprecated("Use `toRequestParameters()` instead.", ReplaceWith("apolloRequest.toRequestParameters().toByteString()"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_3_1)
    fun <D : Operation.Data> buildParamsMap(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
    ): ByteString {
      @Suppress("DEPRECATION")
      return buildParamsMap(operation, customScalarAdapters, autoPersistQueries, sendDocument, false)
    }

    @Deprecated("Use `toRequestParameters()` instead.", ReplaceWith("apolloRequest.toRequestParameters().toByteString()"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    fun <D : Operation.Data> buildParamsMap(
        operation: Operation<D>,
        customScalarAdapters: CustomScalarAdapters,
        autoPersistQueries: Boolean,
        sendDocument: Boolean,
        sendEnhancedClientAwarenessExtensions: Boolean,
    ): ByteString {
      return ApolloRequest.Builder(operation)
          .addExecutionContext(customScalarAdapters)
          .sendApqExtensions(autoPersistQueries)
          .sendDocument(sendDocument)
          .sendEnhancedClientAwareness(sendEnhancedClientAwarenessExtensions)
          .build()
          .toRequestParameters()
          .toByteString()
    }

    @Deprecated("Use `toRequestParameters()` instead.", ReplaceWith("apolloRequest.toRequestParameters().toMap()"))
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    fun <D : Operation.Data> composePayload(
        apolloRequest: ApolloRequest<D>,
    ): Map<String, Any?> {
      return apolloRequest.toRequestParameters().toMap()
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

private fun Map<String, ApolloJsonElement>.asGetParameters(): Map<String, String> {
  return mapValues {
    val v = it.value
    if (v is String) {
      v
    } else {
      buildJsonString {
        writeAny(v)
      }
    }
  }
}