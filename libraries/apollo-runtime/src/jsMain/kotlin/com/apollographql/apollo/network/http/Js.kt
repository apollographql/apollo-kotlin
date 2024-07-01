package com.apollographql.apollo.network.http

import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

@Suppress("NOTHING_TO_INLINE")
internal inline fun dynamicObject(noinline init: dynamic.() -> Unit): dynamic {
  val js = js("{}")
  init(js)
  return js
}

@Suppress("UnsafeCastFromDynamic")
internal fun Uint8Array.asByteArray(): ByteArray {
  return Int8Array(buffer, byteOffset, length).asDynamic()
}

internal external fun setTimeout(functionRef: () -> Unit, delay: Long): Int

internal external fun clearTimeout(timeoutID: Int)

internal external fun fetch(resource: String, options: dynamic): Promise<Response>

internal fun AbortController(): dynamic {
  return js("new AbortController()")
}

internal fun requireNodeFetch(): dynamic {
  return js("eval('require')('node-fetch')")
}

internal external interface ReadableStream<R> {
  fun getReader(): ReadableStreamDefaultReader<R>
}

internal external interface ReadableStreamDefaultReader<R> {
  fun cancel(reason: Any? = definedExternally): Promise<Unit>
  fun read(): Promise<ReadableStreamReadResult<R>>
}

internal external interface ReadableStreamReadResult<T> {
  var done: Boolean
  var value: T
}

/**
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 * From https://github.com/ktorio/ktor/blob/5d9b1c0b975394b7ce3dc2a096e5fb9c7e52ce37/ktor-client/ktor-client-core/js/src/io/ktor/client/engine/js/browser/BrowserFetch.kt#L37
 */
internal suspend fun ReadableStreamDefaultReader<Uint8Array>.readChunk(): Uint8Array? =
    suspendCancellableCoroutine { continuation ->
      read().then {
        val chunk = it.value
        val result = if (it.done) null else chunk
        continuation.resume(result)
      }.catch { cause ->
        continuation.resumeWithException(cause)
      }
    }
