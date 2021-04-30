package com.apollographql.apollo3.api.http

import okio.BufferedSink
import okio.BufferedSource

enum class HttpMethod {
  Get, Post
}

interface HttpBody {
  val contentType: String
  val contentLength: Long
  fun writeTo(bufferedSink: BufferedSink)
}

class HttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val method: HttpMethod,
    val body: HttpBody?
)

class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    /**
     * The actual body
     * It must always be closed if not null
     */
    val body: BufferedSource?,
)