package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_8_3
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
 */
class ApolloRequest<D : Operation.Data>
private constructor(
    val operation: Operation<D>,
    val requestUuid: Uuid,
    override val executionContext: ExecutionContext,
    override val httpMethod: HttpMethod?,
    override val httpHeaders: List<HttpHeader>?,
    override val sendApqExtensions: Boolean?,
    override val sendDocument: Boolean?,
    override val enableAutoPersistedQueries: Boolean?,
    override val canBeBatched: Boolean?,
) : ExecutionOptions {

  fun newBuilder(): Builder<D> = newBuilder(operation)

  @ApolloExperimental
  fun <E : Operation.Data> newBuilder(operation: Operation<E>): Builder<E> {
    return Builder(operation)
        .requestUuid(requestUuid)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)

  }

  class Builder<D : Operation.Data>(
      private var operation: Operation<D>,
  ) : MutableExecutionOptions<Builder<D>> {
    private var requestUuid: Uuid = uuid4()
    override var executionContext: ExecutionContext = ExecutionContext.Empty
      @Deprecated("Use addExecutionContext() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override var httpMethod: HttpMethod? = null
      @Deprecated("Use httpMethod() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun httpMethod(httpMethod: HttpMethod?): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.httpMethod = httpMethod
    }

    override var httpHeaders: List<HttpHeader>? = null
      @Deprecated("Use httpHeaders() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.httpHeaders = httpHeaders
    }

    override fun addHttpHeader(name: String, value: String): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    }

    override var sendApqExtensions: Boolean? = null
      @Deprecated("Use sendApqExtensions() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.sendApqExtensions = sendApqExtensions
    }

    override var sendDocument: Boolean? = null
      @Deprecated("Use sendDocument() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun sendDocument(sendDocument: Boolean?): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.sendDocument = sendDocument
    }

    override var enableAutoPersistedQueries: Boolean? = null
      @Deprecated("Use enableAutoPersistedQueries() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    override var canBeBatched: Boolean? = null
      @Deprecated("Use canBeBatched() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun canBeBatched(canBeBatched: Boolean?): Builder<D> = apply {
      @Suppress("DEPRECATION")
      this.canBeBatched = canBeBatched
    }

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      @Suppress("DEPRECATION")
      this.executionContext = executionContext
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      @Suppress("DEPRECATION")
      this.executionContext = this.executionContext + executionContext
    }

    fun build(): ApolloRequest<D> {
      return ApolloRequest(
          operation = operation,
          requestUuid = requestUuid,
          executionContext = executionContext,
          httpMethod = httpMethod,
          httpHeaders = httpHeaders,
          sendApqExtensions = sendApqExtensions,
          sendDocument = sendDocument,
          enableAutoPersistedQueries = enableAutoPersistedQueries,
          canBeBatched = canBeBatched,
      )
    }
  }
}
