package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.FileUpload
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputType
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.cache.http.HttpCache
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.json.InputFieldJsonWriter
import com.apollographql.apollo.api.internal.json.JsonWriter.Companion.of
import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.request.RequestHeaders
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/**
 * ApolloServerInterceptor is a concrete [ApolloInterceptor] responsible for making the network calls to the
 * server. It is the last interceptor in the chain of interceptors and hence doesn't call
 * [ApolloInterceptorChain.proceedAsync]
 * on the interceptor chain.
 */
class ApolloServerInterceptor(
    val serverUrl: HttpUrl,
    val httpCallFactory: Call.Factory,
    val cachePolicy: HttpCachePolicy.Policy?,
    val prefetch: Boolean,
    val customScalarAdapters: CustomScalarAdapters,
    val logger: ApolloLogger
) : ApolloInterceptor {
  var httpCallRef = AtomicReference<Call?>()

  @Volatile
  var disposed = false

  override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                              dispatcher: Executor, callBack: CallBack) {
    dispatcher.execute { executeHttpCall(request, callBack) }
  }

  override fun dispose() {
    disposed = true
    val httpCall = httpCallRef.getAndSet(null)
    httpCall?.cancel()
  }

  fun executeHttpCall(request: InterceptorRequest, callBack: CallBack) {
    if (disposed) return
    callBack.onFetch(FetchSourceType.NETWORK)
    val httpCall: Call
    httpCall = try {
      if (request.useHttpGetMethodForQueries && request.operation is Query<*, *>) {
        httpGetCall(request.operation, request.cacheHeaders, request.requestHeaders,
            request.sendQueryDocument, request.autoPersistQueries)
      } else {
        httpPostCall(request.operation, request.cacheHeaders, request.requestHeaders,
            request.sendQueryDocument, request.autoPersistQueries)
      }
    } catch (e: IOException) {
      logger.e(e, "Failed to prepare http call for operation %s", request.operation.name().name())
      callBack.onFailure(ApolloNetworkException("Failed to prepare http call", e))
      return
    }
    val previousCall = httpCallRef.getAndSet(httpCall)
    previousCall?.cancel()
    if (httpCall.isCanceled || disposed) {
      httpCallRef.compareAndSet(httpCall, null)
      return
    }
    httpCall.enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        if (disposed) return
        if (httpCallRef.compareAndSet(httpCall, null)) {
          logger.e(e, "Failed to execute http call for operation %s", request.operation.name().name())
          callBack.onFailure(ApolloNetworkException("Failed to execute http call", e))
        }
      }

      override fun onResponse(call: Call, response: Response) {
        if (disposed) return
        if (httpCallRef.compareAndSet(httpCall, null)) {
          callBack.onResponse(InterceptorResponse(response))
          callBack.onCompleted()
        }
      }
    })
  }

  @Throws(IOException::class)
  fun httpGetCall(operation: Operation<*, *>, cacheHeaders: CacheHeaders, requestHeaders: RequestHeaders,
                  writeQueryDocument: Boolean, autoPersistQueries: Boolean): Call {
    val requestBuilder = Request.Builder()
        .url(httpGetUrl(serverUrl, operation, customScalarAdapters, writeQueryDocument, autoPersistQueries))
        .get()
    decorateRequest(requestBuilder, operation, cacheHeaders, requestHeaders)
    return httpCallFactory.newCall(requestBuilder.build())
  }

  @Throws(IOException::class)
  fun httpPostCall(operation: Operation<*, *>, cacheHeaders: CacheHeaders, requestHeaders: RequestHeaders,
                   writeQueryDocument: Boolean, autoPersistQueries: Boolean): Call {
    var requestBody = RequestBody.create(MEDIA_TYPE, httpPostRequestBody(operation, customScalarAdapters,
        writeQueryDocument, autoPersistQueries))
    requestBody = transformToMultiPartIfUploadExists(requestBody, operation)
    val requestBuilder = Request.Builder()
        .url(serverUrl)
        .header(HEADER_CONTENT_TYPE, CONTENT_TYPE)
        .post(requestBody)
    decorateRequest(requestBuilder, operation, cacheHeaders, requestHeaders)
    return httpCallFactory.newCall(requestBuilder.build())
  }

  @Throws(IOException::class)
  fun decorateRequest(requestBuilder: Request.Builder, operation: Operation<*, *>, cacheHeaders: CacheHeaders,
                      requestHeaders: RequestHeaders) {
    requestBuilder
        .header(HEADER_ACCEPT_TYPE, ACCEPT_TYPE)
        .header(HEADER_APOLLO_OPERATION_ID, operation.operationId())
        .header(HEADER_APOLLO_OPERATION_NAME, operation.name().name())
        .tag(operation.operationId())
    for (header in requestHeaders.headers()) {
      val value = requestHeaders.headerValue(header)
      requestBuilder.header(header, value)
    }
    if (cachePolicy != null) {
      val skipCacheHttpResponse = "true".equals(cacheHeaders.headerValue(
          ApolloCacheHeaders.DO_NOT_STORE), ignoreCase = true)
      val cacheKey = cacheKey(operation, customScalarAdapters)
      requestBuilder
          .header(HttpCache.CACHE_KEY_HEADER, cacheKey)
          .header(HttpCache.CACHE_FETCH_STRATEGY_HEADER, cachePolicy.fetchStrategy.name)
          .header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER, cachePolicy.expireTimeoutMs().toString())
          .header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER, java.lang.Boolean.toString(cachePolicy.expireAfterRead))
          .header(HttpCache.CACHE_PREFETCH_HEADER, java.lang.Boolean.toString(prefetch))
          .header(HttpCache.CACHE_DO_NOT_STORE, java.lang.Boolean.toString(skipCacheHttpResponse))
    }
  }

  class FileUploadMeta internal constructor(val key: String, val mimetype: String, val fileUpload: FileUpload)
  companion object {
    const val HEADER_ACCEPT_TYPE = "Accept"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_APOLLO_OPERATION_ID = "X-APOLLO-OPERATION-ID"
    const val HEADER_APOLLO_OPERATION_NAME = "X-APOLLO-OPERATION-NAME"
    const val ACCEPT_TYPE = "application/json"
    const val CONTENT_TYPE = "application/json"
    val MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8")

    @Throws(IOException::class)
    fun cacheKey(operation: Operation<*, *>, customScalarAdapters: CustomScalarAdapters?): String {
      return httpPostRequestBody(operation, customScalarAdapters, true, true).md5().hex()
    }

    @Throws(IOException::class)
    fun httpPostRequestBody(operation: Operation<*, *>, customScalarAdapters: CustomScalarAdapters?,
                            writeQueryDocument: Boolean, autoPersistQueries: Boolean): ByteString {
      return operation.composeRequestBody(autoPersistQueries, writeQueryDocument, customScalarAdapters!!)
    }

    @Throws(IOException::class)
    fun httpGetUrl(serverUrl: HttpUrl, operation: Operation<*, *>,
                   customScalarAdapters: CustomScalarAdapters?, writeQueryDocument: Boolean,
                   autoPersistQueries: Boolean): HttpUrl {
      val urlBuilder = serverUrl.newBuilder()
      if (!autoPersistQueries || writeQueryDocument) {
        urlBuilder.addQueryParameter("query", operation.queryDocument())
      }
      if (operation.variables() !== Operation.EMPTY_VARIABLES) {
        addVariablesUrlQueryParameter(urlBuilder, operation, customScalarAdapters)
      }
      urlBuilder.addQueryParameter("operationName", operation.name().name())
      if (autoPersistQueries) {
        addExtensionsUrlQueryParameter(urlBuilder, operation)
      }
      return urlBuilder.build()
    }

    @Throws(IOException::class)
    fun addVariablesUrlQueryParameter(urlBuilder: HttpUrl.Builder, operation: Operation<*, *>,
                                      customScalarAdapters: CustomScalarAdapters?) {
      val buffer = Buffer()
      val jsonWriter = of(buffer)
      jsonWriter.serializeNulls = true
      jsonWriter.beginObject()
      operation.variables().marshaller().marshal(InputFieldJsonWriter(jsonWriter, customScalarAdapters!!))
      jsonWriter.endObject()
      jsonWriter.close()
      urlBuilder.addQueryParameter("variables", buffer.readUtf8())
    }

    @Throws(IOException::class)
    fun addExtensionsUrlQueryParameter(urlBuilder: HttpUrl.Builder, operation: Operation<*, *>) {
      val buffer = Buffer()
      val jsonWriter = of(buffer)
      jsonWriter.serializeNulls = true
      jsonWriter.beginObject()
      jsonWriter.name("persistedQuery")
          .beginObject()
          .name("version").value(1)
          .name("sha256Hash").value(operation.operationId())
          .endObject()
      jsonWriter.endObject()
      jsonWriter.close()
      urlBuilder.addQueryParameter("extensions", buffer.readUtf8())
    }

    private fun recursiveGetUploadData(value: Any?, variableName: String, allUploads: ArrayList<FileUploadMeta>) {
      when (value) {
        is InputType -> {
          // Input object
          try {
            val fields = value.javaClass.declaredFields
            for (field in fields) {
              field.isAccessible = true
              val subValue = field[value]
              val key = field.name
              recursiveGetUploadData(subValue, "$variableName.$key", allUploads)
            }
          } catch (e: IllegalAccessException) {
            // never happen
          }
        }
        is Input<*> -> {
          val unwrappedValue = value.value
          recursiveGetUploadData(unwrappedValue, variableName, allUploads)
        }
        is FileUpload -> {
          val upload = value
          allUploads.add(FileUploadMeta(variableName, upload.mimetype, upload))
        }
        is Array<*> -> {
          // TODO: when does this case happen?
          var varFileIndex = 0
          value.filterIsInstance<FileUpload>().forEach { upload ->
            val key = "$variableName.$varFileIndex"
            allUploads.add(FileUploadMeta(key, upload.mimetype, upload))
            println(key)
            varFileIndex++
          }
        }
        is Collection<*> -> {
          value.forEachIndexed { index, element ->
            recursiveGetUploadData(element, "$variableName.$index", allUploads)
          }
        }
      }
    }

    @Throws(IOException::class)
    fun transformToMultiPartIfUploadExists(originalBody: RequestBody?, operation: Operation<*, *>): RequestBody? {
      val allUploads = ArrayList<FileUploadMeta>()
      for (variableName in operation.variables().valueMap().keys) {
        val value = operation.variables().valueMap()[variableName]
        recursiveGetUploadData(value, "variables.$variableName", allUploads)
      }
      return if (allUploads.isEmpty()) {
        originalBody
      } else {
        httpMultipartRequestBody(originalBody, allUploads)
      }
    }

    @Throws(IOException::class)
    fun httpMultipartRequestBody(operations: RequestBody?, fileUploadMetaList: ArrayList<FileUploadMeta>): RequestBody {
      val buffer = Buffer()
      val jsonWriter = of(buffer)
      jsonWriter.beginObject()
      fileUploadMetaList.forEachIndexed { i, fileUploadMeta ->
        jsonWriter.name(i.toString()).beginArray()
        jsonWriter.value(fileUploadMeta.key)
        jsonWriter.endArray()
      }

      jsonWriter.endObject()
      jsonWriter.close()
      val multipartBodyBuilder = MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("operations", null, operations)
          .addFormDataPart("map", null, RequestBody.create(MEDIA_TYPE, buffer.readByteString()))

      fileUploadMetaList.forEachIndexed { i, fileUploadMeta ->
        val file = fileUploadMeta.fileUpload.filePath?.let { File(it) }
        val mimetype = MediaType.parse(fileUploadMeta.fileUpload.mimetype)
        if (file != null) {
          multipartBodyBuilder.addFormDataPart(
              i.toString(),
              file.name,
              RequestBody.create(mimetype, file)
          )
        } else {
          multipartBodyBuilder.addFormDataPart(
              i.toString(),
              fileUploadMeta.fileUpload.fileName(),
              object : RequestBody() {
                override fun contentType() = mimetype
                override fun contentLength() = fileUploadMeta.fileUpload.contentLength()
                override fun writeTo(sink: BufferedSink) = fileUploadMeta.fileUpload.writeTo(sink)
              }
          )
        }
      }
      return multipartBodyBuilder.build()
    }
  }
}
