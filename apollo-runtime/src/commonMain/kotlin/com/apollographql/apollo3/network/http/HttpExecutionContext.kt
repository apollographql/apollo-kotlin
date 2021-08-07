package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.http.HttpHeader

data class HttpInfo(
    val statusCode: Int,
    val headers: List<HttpHeader>
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpInfo>
}
