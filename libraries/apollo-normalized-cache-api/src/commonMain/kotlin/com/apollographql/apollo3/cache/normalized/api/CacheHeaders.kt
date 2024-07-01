package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.api.Operation
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A key/value collection which is sent with [Record] from a [Operation] to the [NormalizedCache].
 *
 * For headers which the default [NormalizedCache] respect, see [ApolloCacheHeaders].
 */
class CacheHeaders internal constructor(private val headerMap: Map<String, String>) {

  class Builder {
    private val headerMap = mutableMapOf<String, String>()

    fun addHeader(headerName: String, headerValue: String) = apply {
      headerMap[headerName] = headerValue
    }

    fun addHeaders(headerMap: Map<String, String>) = apply {
      this.headerMap.putAll(headerMap)
    }

    fun build() = CacheHeaders(headerMap)
  }

  fun newBuilder(): Builder = builder().addHeaders(headerMap)

  fun headerValue(header: String): String? = headerMap[header]

  fun hasHeader(headerName: String): Boolean = headerMap.containsKey(headerName)

  companion object {
    @JvmStatic
    fun builder() = Builder()

    @JvmField
    val NONE = CacheHeaders(emptyMap())
  }
  operator fun plus(cacheHeaders: CacheHeaders): CacheHeaders {
    return newBuilder().addHeaders(cacheHeaders.headerMap).build()
  }
}
