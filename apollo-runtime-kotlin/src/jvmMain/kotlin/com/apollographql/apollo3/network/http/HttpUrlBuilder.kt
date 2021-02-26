package com.apollographql.apollo3.network.http

import okhttp3.HttpUrl

actual class HttpUrlBuilder actual constructor(val baseUrl: String, val queryParameters: Map<String, String>) {
  actual fun build(): String {
    return HttpUrl.parse(baseUrl)!!.newBuilder()
        .apply {
          queryParameters.forEach {
            addQueryParameter(it.key, it.value)
          }
        }.build()
        .toString()
  }
}