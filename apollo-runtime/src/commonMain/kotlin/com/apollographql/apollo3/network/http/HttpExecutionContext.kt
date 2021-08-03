package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext
import com.apollographql.apollo3.api.http.HttpHeader

data class HttpRequestParameters(val headers: Map<String, String>) : RequestContext(HttpRequestParameters) {
  companion object Key : ExecutionContext.Key<HttpRequestParameters>
}

fun HttpRequestParameters?.withHeader(name: String, value: String) = (this ?: HttpRequestParameters(emptyMap())).let {
  it.copy(headers = it.headers + (name to value))
}

data class HttpResponseInfo(
    val statusCode: Int,
    val headers: List<HttpHeader>
) : ResponseContext(HttpResponseInfo) {
  companion object Key : ExecutionContext.Key<HttpResponseInfo>
}


