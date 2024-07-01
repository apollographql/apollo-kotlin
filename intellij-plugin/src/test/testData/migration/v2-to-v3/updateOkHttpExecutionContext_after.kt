package com.example

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.network.http.HttpInfo

suspend fun main() {
  val response: ApolloResponse<out Any?>? = null
  val responseCode: Int? = response!!.executionContext[HttpInfo]?.statusCode
  val headers = response.executionContext[HttpInfo]?.headers
}
