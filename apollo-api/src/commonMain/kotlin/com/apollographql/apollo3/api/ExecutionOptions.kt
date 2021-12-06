package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod

interface ExecutionOptions {
  val executionContext: ExecutionContext

  val httpMethod: HttpMethod

  val httpHeaders: List<HttpHeader>

  val sendApqExtensions: Boolean

  val sendDocument: Boolean

  companion object {

    val defaultHttpMethod = HttpMethod.Post
    val defaultSendApqExtensions = false
    val defaultSendDocument = true
  }
}


interface MutableExecutionOptions<T> : ExecutionOptions {
  fun addExecutionContext(executionContext: ExecutionContext): T

  fun httpMethod(httpMethod: HttpMethod): T

  fun httpHeaders(httpHeaders: List<HttpHeader>): T

  fun sendApqExtensions(sendApqExtensions: Boolean): T

  fun sendDocument(sendDocument: Boolean): T
}

class DefaultExecutionOptions(
    override val executionContext: ExecutionContext,
    override val httpMethod: HttpMethod,
    override val httpHeaders: List<HttpHeader>,
    override val sendApqExtensions: Boolean,
    override val sendDocument: Boolean,
): ExecutionOptions