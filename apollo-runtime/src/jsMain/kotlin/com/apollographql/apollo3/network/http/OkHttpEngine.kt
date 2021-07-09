package com.apollographql.apollo3.network.http

import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse

actual class DefaultHttpEngine actual constructor(connectTimeoutMillis: Long, readTimeoutMillis: Long) : HttpEngine {
  override suspend fun execute(request: HttpRequest): HttpResponse {
    TODO()
  }

  override fun dispose() {
    TODO()
  }
}