package com.apollographql.apollo3.cache.normalized.api

import com.apollographql.apollo3.api.Operation
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

  /**
   * @return A [CacheHeaders.Builder] with a copy of this [CacheHeaders] values.
   */
  fun toBuilder(): Builder = builder().addHeaders(headerMap)

  fun headerValue(header: String): String? = headerMap[header]

  fun hasHeader(headerName: String): Boolean = headerMap.containsKey(headerName)

  companion object {
    @JvmStatic
    fun builder() = Builder()

    @JvmField
    val NONE = CacheHeaders(emptyMap())
  }

}
