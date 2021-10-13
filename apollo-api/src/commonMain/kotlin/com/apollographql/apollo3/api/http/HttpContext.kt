package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionParameters
import com.apollographql.apollo3.api.HasExecutionContext


fun HasExecutionContext.httpMethod() = executionContext[HttpMethodContext]?.value ?: HttpMethod.Post
fun HasExecutionContext.httpHeaders() = executionContext[HttpHeadersContext]?.value ?: emptyList()
fun HasExecutionContext.sendApqExtensions() = executionContext[SendApqExtensionsContext]?.value ?: false

fun HasExecutionContext.sendDocument() = executionContext[SendDocumentContext]?.value ?: true

/**
 * Configures whether the request should use GET or POST
 * Usually, POST request can transfer bigger GraphQL documents but are more difficult to cache
 *
 * Default: [HttpMethod.Post]
 */
fun <T> ExecutionParameters<T>.withHttpMethod(httpMethod: HttpMethod) where T : ExecutionParameters<T> = withExecutionContext(executionContext + HttpMethodContext(httpMethod))

/**
 *
 */
fun <T> ExecutionParameters<T>.withHttpHeaders(httpHeaders: List<HttpHeader>) where T : ExecutionParameters<T> = withExecutionContext(
    executionContext + HttpHeadersContext(httpHeaders() + httpHeaders)
)
fun <T> ExecutionParameters<T>.withHttpHeader(httpHeader: HttpHeader) where T : ExecutionParameters<T> = withExecutionContext(
    executionContext + HttpHeadersContext(httpHeaders() + httpHeader)
)
fun <T> ExecutionParameters<T>.withHttpHeader(name: String, value: String) where T : ExecutionParameters<T> = withHttpHeader(
    HttpHeader(name, value)
)

fun <T> ExecutionParameters<T>.withSendApqExtensions(sendApqExtensions: Boolean) where T : ExecutionParameters<T> = withExecutionContext(executionContext + SendApqExtensionsContext(sendApqExtensions))
fun <T> ExecutionParameters<T>.withSendDocument(sendDocument: Boolean) where T : ExecutionParameters<T> = withExecutionContext(executionContext + SendDocumentContext(sendDocument))


/**
 * [ExecutionContext] for HTTP parameters. That's a lot of wrapper classes but it's hard to avoid them if we want to be able to override
 * parameters individually
 */
internal class HttpMethodContext(val value: HttpMethod) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpMethodContext>
}

internal class SendApqExtensionsContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<SendApqExtensionsContext>
}

internal class SendDocumentContext(val value: Boolean) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<SendDocumentContext>
}

internal class HttpHeadersContext(val value: List<HttpHeader>) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<HttpHeadersContext>
}