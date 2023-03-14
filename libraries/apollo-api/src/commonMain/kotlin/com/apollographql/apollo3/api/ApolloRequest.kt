package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
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
    override val ignorePartialData: Boolean?,
    val additionalHttpHeaders: List<HttpHeader>?,
    @ApolloInternal
    val useV3ExceptionHandling: Boolean?,
) : ExecutionOptions {

  fun newBuilder(): Builder<D> = newBuilder(operation)

  @ApolloExperimental
  fun <E: Operation.Data> newBuilder(operation: Operation<E>): Builder<E> {
    @Suppress("DEPRECATION")
    return Builder(operation)
        .requestUuid(requestUuid)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .additionalHttpHeaders(additionalHttpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .ignorePartialData(ignorePartialData)
        .useV3ExceptionHandling(useV3ExceptionHandling)
  }

  class Builder<D : Operation.Data>(
      private var operation: Operation<D>,
  ) : MutableExecutionOptions<Builder<D>> {
    private var requestUuid: Uuid = uuid4()
    override var executionContext: ExecutionContext = ExecutionContext.Empty

    override var httpMethod: HttpMethod? = null

    override fun httpMethod(httpMethod: HttpMethod?): Builder<D> = apply {
      this.httpMethod = httpMethod
    }

    override var httpHeaders: List<HttpHeader>? = null

    val additionalHttpHeaders: List<HttpHeader>?
      get() = additionalHttpHeaders_

    private var additionalHttpHeaders_: List<HttpHeader>? = null

    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder<D> = apply {
      this.httpHeaders = httpHeaders
    }

    fun additionalHttpHeaders(additionalHttpHeaders: List<HttpHeader>?) = apply {
      this.additionalHttpHeaders_ = additionalHttpHeaders
    }

    override fun addHttpHeader(name: String, value: String): Builder<D> = apply {
      this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    }

    override var sendApqExtensions: Boolean? = null

    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder<D> = apply {
      this.sendApqExtensions = sendApqExtensions
    }

    override var sendDocument: Boolean? = null

    override fun sendDocument(sendDocument: Boolean?): Builder<D> = apply {
      this.sendDocument = sendDocument
    }

    override var enableAutoPersistedQueries: Boolean? = null

    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder<D> = apply {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    override var canBeBatched: Boolean? = null

    override fun canBeBatched(canBeBatched: Boolean?): Builder<D> = apply {
      this.canBeBatched = canBeBatched
    }

    override var ignorePartialData: Boolean? = null

    override fun ignorePartialData(ignorePartialData: Boolean?): Builder<D> = apply {
      this.ignorePartialData = ignorePartialData
    }

    private var useV3ExceptionHandling: Boolean? = null

    @ApolloInternal
    fun useV3ExceptionHandling(useV3ExceptionHandling: Boolean?): Builder<D> = apply {
      this.useV3ExceptionHandling = useV3ExceptionHandling
    }

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = executionContext
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun build(): ApolloRequest<D> {
      @Suppress("DEPRECATION")
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
          ignorePartialData = ignorePartialData,
          additionalHttpHeaders = additionalHttpHeaders,
          useV3ExceptionHandling = useV3ExceptionHandling,
      )
    }
  }
}
