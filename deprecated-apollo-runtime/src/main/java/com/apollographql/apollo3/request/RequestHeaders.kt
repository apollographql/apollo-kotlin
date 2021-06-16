package com.apollographql.apollo3.request

import java.util.LinkedHashMap

/**
 * A key/value collection of HTTP headers which are added to a request.
 */
class RequestHeaders internal constructor(private val headerMap: Map<String, String>) {
  class Builder {
    private val headerMap: MutableMap<String, String> = LinkedHashMap()
    fun addHeader(headerName: String, headerValue: String): Builder {
      headerMap[headerName] = headerValue
      return this
    }

    fun addHeaders(headerMap: Map<String, String>?): Builder {
      this.headerMap.putAll(headerMap!!)
      return this
    }

    fun build(): RequestHeaders {
      return RequestHeaders(headerMap)
    }
  }

  /**
   * @return A [RequestHeaders.Builder] with a copy of this [RequestHeaders] values.
   */
  fun toBuilder(): Builder {
    val builder = builder()
    builder.addHeaders(headerMap)
    return builder
  }

  fun headers(): Set<String> {
    return headerMap.keys
  }

  fun headerValue(header: String): String? {
    return headerMap[header]
  }

  fun hasHeader(headerName: String): Boolean {
    return headerMap.containsKey(headerName)
  }

  companion object {
    fun builder(): Builder {
      return Builder()
    }

    val NONE = RequestHeaders(emptyMap())
  }
}