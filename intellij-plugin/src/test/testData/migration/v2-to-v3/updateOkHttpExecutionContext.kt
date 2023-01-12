package com.example

import com.apollographql.apollo.api.Response
import com.apollographql.apollo.http.OkHttpExecutionContext

suspend fun main() {
  val response: Response<out Any?>? = null
  val responseCode: Int? = response!!.executionContext[OkHttpExecutionContext]?.response?.code()
  val headers = response.executionContext[OkHttpExecutionContext]?.response?.headers()
}
