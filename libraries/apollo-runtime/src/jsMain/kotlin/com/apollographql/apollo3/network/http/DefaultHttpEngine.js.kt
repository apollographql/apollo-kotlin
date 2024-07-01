package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.internal.isNode
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.BufferedSource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.Response
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * @param timeoutMillis: The timeout in milliseconds used both for the connection and the request.
 */
actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = JsHttpEngine(timeoutMillis)

/**
 * @param connectTimeoutMillis The connection timeout in milliseconds. The connection timeout is the time period in which a client should establish a connection with a server.
 * @param readTimeoutMillis The request timeout in milliseconds. The request timeout is the time period required to process an HTTP call: from sending a request to receiving a response.
 */
fun DefaultHttpEngine(connectTimeoutMillis: Long, readTimeoutMillis: Long): HttpEngine =
    JsHttpEngine(connectTimeoutMillis, readTimeoutMillis)

private class JsHttpEngine(
    private val connectTimeoutMillis: Long,
    private val readTimeoutMillis: Long,
) : HttpEngine {
  constructor(timeoutMillis: Long) : this(timeoutMillis, timeoutMillis)

  private val nodeFetch: dynamic = if (isNode) requireNodeFetch() else null

  @Suppress("UnsafeCastFromDynamic")
  override suspend fun execute(request: HttpRequest): HttpResponse {
    val abortController = AbortController()
    val connectTimeoutId = setTimeout({ abortController.abort() }, connectTimeoutMillis)

    val fetchOptions = request.toFetchOptions(abortSignal = abortController.signal)
    val responsePromise: Promise<Response> = if (isNode) {
      nodeFetch(
          resource = request.url,
          options = fetchOptions
      )
    } else {
      fetch(
          resource = request.url,
          options = fetchOptions
      )
    }
    return try {
      val response = responsePromise.await()
      clearTimeout(connectTimeoutId)
      val responseBodySource = if (isNode) {
        readBodyNode(response.body, readTimeoutMillis, abortController)
      } else {
        readBodyBrowser(response.body, readTimeoutMillis, abortController)
      }
      HttpResponse.Builder(response.status.toInt())
          .body(responseBodySource)
          .apply {
            @Suppress("UnsafeCastFromDynamic")
            response.headers.asDynamic().forEach { value: String, key: String ->
              addHeader(key, value)
            }
          }
          .build()
    } catch (t: Throwable) {
      if (t is CancellationException) {
        abortController.abort()
        throw t
      } else {
        throw ApolloNetworkException("Failed to execute GraphQL http network request", t)
      }
    }
  }

  override fun close() {
  }
}

private fun HttpRequest.toFetchOptions(abortSignal: dynamic): dynamic {
  val method = when (method) {
    HttpMethod.Get -> "GET"
    HttpMethod.Post -> "POST"
  }
  val headers = js("({})")
  for (header in this.headers) {
    headers[header.name] = header.value
  }
  val bodyBytes: ByteArray? = body?.let { body ->
    headers["Content-Type"] = body.contentType
    body.contentLength.takeIf { it >= 0 }?.let { contentLength ->
      headers["Content-Length"] = contentLength.toString()
    }
    val bodyBuffer = Buffer()
    body.writeTo(bodyBuffer)
    bodyBuffer.readByteArray()
  }
  return dynamicObject {
    this.signal = abortSignal
    this.method = method
    this.headers = headers
    if (bodyBytes != null) {
      this.body = bodyBytes
    }
  }
}

@Suppress("UnsafeCastFromDynamic")
private suspend fun readBodyNode(body: dynamic, readTimeoutMillis: Long, abortController: dynamic): BufferedSource {
  var readTimeoutId = setTimeout({ abortController.abort() }, readTimeoutMillis)
  val bufferedSource = Buffer()
  return suspendCancellableCoroutine { continuation ->
    body.on("data") { chunk: ArrayBuffer ->
      clearTimeout(readTimeoutId)
      readTimeoutId = setTimeout({ abortController.abort() }, readTimeoutMillis)
      val chunkBytes = Uint8Array(chunk).asByteArray()
      bufferedSource.write(chunkBytes)
    }
    body.on("end") {
      continuation.resume(bufferedSource)
    }
    body.on("error") { error: Throwable ->
      continuation.resumeWithException(error)
    }
    Unit
  }
}

@Suppress("UnsafeCastFromDynamic")
private suspend fun readBodyBrowser(body: dynamic, readTimeoutMillis: Long, abortController: dynamic): BufferedSource {
  var readTimeoutId = setTimeout({ abortController.abort() }, readTimeoutMillis)
  val bufferedSource = Buffer()
  val stream: ReadableStream<Uint8Array> = body ?: return Buffer()
  val reader = stream.getReader()
  while (true) {
    try {
      val chunk: Uint8Array? = reader.readChunk()
      clearTimeout(readTimeoutId)
      readTimeoutId = setTimeout({ abortController.abort() }, readTimeoutMillis)
      if (chunk == null) {
        break
      }
      bufferedSource.write(chunk.asByteArray())
    } catch (cause: Throwable) {
      reader.cancel(cause)
      throw cause
    }
  }
  return bufferedSource
}
