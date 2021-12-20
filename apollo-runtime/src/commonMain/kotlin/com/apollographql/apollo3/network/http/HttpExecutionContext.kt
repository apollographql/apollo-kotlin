package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.http.HttpHeader

class HttpInfo @Deprecated("HttpInfo is only to be constructed internally. Declare your own class if needed") constructor(
    val startMillis: Long,
    val endMillis: Long,
    val statusCode: Int,
    val headers: List<HttpHeader>
) : ExecutionContext.Element {

  @Deprecated("Use startMillis instead", ReplaceWith("startMillis"))
  val millisStart: Long
    get() = startMillis

  @Deprecated("Use endMillis instead", ReplaceWith("endMillis"))
  val millisEnd: Long
    get() = endMillis

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpInfo>
}
