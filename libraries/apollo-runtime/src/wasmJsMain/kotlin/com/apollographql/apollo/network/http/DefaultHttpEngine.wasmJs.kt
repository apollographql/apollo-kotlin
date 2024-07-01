package com.apollographql.apollo.network.http

import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.ApolloNetworkException
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun DefaultHttpEngine(timeoutMillis: Long): HttpEngine = WasmHttpEngine()

private class WasmHttpEngine : HttpEngine {
  override suspend fun execute(request: HttpRequest): HttpResponse {
    val controller = newAbortController()
    val init = makeJsObject<RequestInit>()

    init.signal = controller.signal
    init.headers = makeJsObject<JsAny>().apply {
      request.headers.forEach {
        set(it.name, it.value)
      }
      if (request.body != null) {
        set("content-type", request.body?.contentType ?: "")
        request.body?.contentLength?.toString()?.also {
          set("content-length", it)
        }
      }
    }
    init.method = request.method.name
    init.signal = controller.signal
    request.body?.let { httpBody ->
      init.body = Buffer().also { httpBody.writeTo(it) }.readByteArray().asJsArray()
    }

    val response = suspendCancellableCoroutine<Response> { continuation ->
      continuation.invokeOnCancellation {
        controller.abort()
      }

      fetch(request.url, init).then(
          onFulfilled = {
            continuation.resume(it)
            null
          },
          onRejected = {
            continuation.resumeWith(Result.failure(ApolloNetworkException("Cannot fetch", it)))
            null
          }
      )
    }

    return HttpResponse.Builder(response.status.toInt())
        .headers(response.headers.toHttpHeaders())
        .apply {
          response.body?.toBuffer()?.also {
            body(it)
          }
        }
        .build()
  }

  override fun close() {
  }
}

private suspend fun JsAny.toBuffer(): Buffer {
  val stream: ReadableStream<Uint8Array?> = this.unsafeCast()
  val buffer = Buffer()
  val reader = stream.getReader()

  while (true) {
    try {
      val chunk = reader.readChunk() ?: break
      buffer.write(chunk.asByteArray())
    } catch (cause: Throwable) {
      reader.cancel(cause.toJsReference())
      throw cause
    }
  }

  return buffer
}

internal suspend fun ReadableStreamDefaultReader<Uint8Array?>.readChunk(): Uint8Array? =
    suspendCancellableCoroutine<Uint8Array?> { continuation ->
      read().then { stream: ReadableStreamReadResult<Uint8Array?> ->
        val chunk = stream.value
        val result = if (stream.done || chunk == null) null else chunk
        continuation.resumeWith(Result.success(result))
        null
      }.catch { cause: JsAny ->
        continuation.resumeWithException(ApolloNetworkException("cannot read body", cause))
        null
      }
    }


private fun getKeys(headers: org.w3c.fetch.Headers): JsArray<JsString> =
    js("Array.from(headers.keys())")

internal fun org.w3c.fetch.Headers.toHttpHeaders(): List<HttpHeader> {
  val keys = getKeys(this)

  return buildList {
    for (i in 0 until keys.length) {
      val key = keys[i].toString()
      val value = this@toHttpHeaders.get(key)!!
      this@buildList.add(HttpHeader(key, value))
    }
  }
}

private fun newAbortController(): AbortController = js("new AbortController()")
