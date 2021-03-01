package com.apollographql.apollo3.cache.http

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.Source
import okio.buffer

internal class CacheResponseBody(responseBodySource: Source, contentType: String?, contentLength: String?) : ResponseBody() {
  private val responseBodySource: BufferedSource
  private val contentType: String?
  private val contentLength: String?
  override fun contentType(): MediaType? {
    return if (contentType != null) MediaType.parse(contentType) else null
  }

  override fun contentLength(): Long {
    return try {
      contentLength?.toLong() ?: -1
    } catch (e: NumberFormatException) {
      -1
    }
  }

  override fun source(): BufferedSource {
    return responseBodySource
  }

  init {
    this.responseBodySource = responseBodySource.buffer()
    this.contentType = contentType
    this.contentLength = contentLength
  }
}