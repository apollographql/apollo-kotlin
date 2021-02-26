package com.apollographql.apollo3.network.http

expect class HttpUrlBuilder(baseUrl: String, queryParameters: Map<String, String>) {
  fun build(): String
}