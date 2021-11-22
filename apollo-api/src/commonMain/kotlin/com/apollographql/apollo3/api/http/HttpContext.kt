@file:JvmName("HttpContext")
package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import kotlin.jvm.JvmName


val HasExecutionContext.httpMethod get() = executionContext[HttpMethodContext]?.value ?: HttpMethod.Post
val HasExecutionContext.httpHeaders get() = executionContext[HttpHeadersContext]?.value ?: emptyList()
val HasExecutionContext.sendApqExtensions get() = executionContext[SendApqExtensionsContext]?.value ?: false
val HasExecutionContext.sendDocument get() = executionContext[SendDocumentContext]?.value ?: true

/**
 * Configures whether the request should use GET or POST
 * Usually, POST request can transfer bigger GraphQL documents but are more difficult to cache
 *
 * Default: [HttpMethod.Post]
 */
fun <T> HasMutableExecutionContext<T>.httpMethod(httpMethod: HttpMethod) where T : HasMutableExecutionContext<T> = addExecutionContext(executionContext + HttpMethodContext(httpMethod))

/**
 * Add HTTP headers to be sent with the request.
 */
fun <T> HasMutableExecutionContext<T>.addHttpHeaders(httpHeaders: List<HttpHeader>) where T : HasMutableExecutionContext<T> = addExecutionContext(
    executionContext + HttpHeadersContext(this.httpHeaders + httpHeaders)
)

/**
 * Add an HTTP header to be sent with the request.
 */
fun <T> HasMutableExecutionContext<T>.addHttpHeader(httpHeader: HttpHeader) where T : HasMutableExecutionContext<T> = addExecutionContext(
    executionContext + HttpHeadersContext(httpHeaders + httpHeader)
)

/**
 * Add an HTTP header to be sent with the request.
 */
fun <T> HasMutableExecutionContext<T>.addHttpHeader(name: String, value: String) where T : HasMutableExecutionContext<T> = addHttpHeader(
    HttpHeader(name, value)
)

fun <T> HasMutableExecutionContext<T>.sendApqExtensions(sendApqExtensions: Boolean) where T : HasMutableExecutionContext<T> = addExecutionContext(executionContext + SendApqExtensionsContext(sendApqExtensions))
fun <T> HasMutableExecutionContext<T>.sendDocument(sendDocument: Boolean) where T : HasMutableExecutionContext<T> = addExecutionContext(executionContext + SendDocumentContext(sendDocument))


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


@Deprecated("Please use addHttpHeaders instead. This will be removed in v3.0.0.", ReplaceWith("addHttpHeaders(httpHeaders)"))
fun <T> HasMutableExecutionContext<T>.httpHeaders(httpHeaders: List<HttpHeader>) where T : HasMutableExecutionContext<T> = addHttpHeaders(httpHeaders)

@Deprecated("Please use addHttpHeader instead. This will be removed in v3.0.0.", ReplaceWith("addHttpHeader(httpHeader)"))
fun <T> HasMutableExecutionContext<T>.httpHeader(httpHeader: HttpHeader) where T : HasMutableExecutionContext<T> = addHttpHeader(httpHeader)

@Deprecated("Please use addHttpHeader instead. This will be removed in v3.0.0.", ReplaceWith("addHttpHeader(name, value)"))
fun <T> HasMutableExecutionContext<T>.httpHeader(
    name: String,
    value: String,
) where T : HasMutableExecutionContext<T> = addHttpHeader(name, value)
