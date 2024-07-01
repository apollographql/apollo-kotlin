package com.example

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.network.http.HttpInfo

suspend fun main() {
  val response: ApolloResponse<out Any?>? = null
  val responseCode: Int? = response!!.executionContext[HttpInfo]?.statusCode
  val headers = response.executionContext[HttpInfo]?.headers
}
