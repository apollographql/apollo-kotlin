package com.apollographql.apollo3.network.http

import okhttp3.HttpUrl

actual fun buildUrl(baseUrl: String, queryParameters: Map<String, String>): String {
  return HttpUrl.parse(baseUrl)!!.newBuilder()
      .apply {
        queryParameters.forEach {
          addQueryParameter(it.key, it.value)
        }
      }.build()
      .toString()
}
