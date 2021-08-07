package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.http.HttpHeader

data class HttpRequestParameters(val headers: Map<String, String>) : ExecutionContext.Element{
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpRequestParameters>
}

fun HttpRequestParameters?.withHeader(name: String, value: String) = (this ?: HttpRequestParameters(emptyMap())).let {
  it.copy(headers = it.headers + (name to value))
}

data class HttpResponseInfo(
    val statusCode: Int,
    val headers: List<HttpHeader>
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpResponseInfo>
}


