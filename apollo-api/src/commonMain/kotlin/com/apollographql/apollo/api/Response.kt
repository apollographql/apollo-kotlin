package com.apollographql.apollo.api

import kotlin.js.JsName
import kotlin.jvm.JvmStatic

/**
 * Represents either a successful or failed response received from the GraphQL server.
 */
data class Response<T>(
    /**
     * GraphQL operation this response represents of
     */
    val operation: Operation<*, *, *>,

    /**
     * Parsed response of GraphQL [operation] execution.
     * Can be `null` in case if [operation] execution failed.
     */
    val data: T?,

    /**
     * GraphQL [operation] execution errors returned by the server to let client know that something has gone wrong.
     */
    val errors: List<Error>? = null,

    /**
     * Set of request object keys to identify cache records to invalidate.
     * Used by normalized cache implementation.
     */
    val dependentKeys: Set<String> = emptySet(),

    /**
     * Indicates if response is resolved from the cache.
     */
    val isFromCache: Boolean = false,

    /**
     * Extensions of GraphQL protocol, arbitrary map of key [String] / value [Any] sent by server along with the response.
     */
    val extensions: Map<String, Any?> = emptyMap(),

    /**
     * The context of GraphQL [operation] execution.
     */
    val executionContext: ExecutionContext = ExecutionContext.Empty
) {

  constructor(builder: Builder<T>) : this(
      operation = builder.operation,
      data = builder.data,
      errors = builder.errors,
      dependentKeys = builder.dependentKeys.orEmpty(),
      isFromCache = builder.fromCache,
      extensions = builder.extensions.orEmpty(),
      executionContext = builder.executionContext
  )

  fun hasErrors(): Boolean = !errors.isNullOrEmpty()

  fun toBuilder(): Builder<T> = Builder<T>(operation)
      .data(data)
      .errors(errors)
      .dependentKeys(dependentKeys)
      .fromCache(isFromCache)
      .extensions(extensions)
      .executionContext(executionContext)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Response<*>) return false

    if (operation != other.operation) return false
    if (data != other.data) return false
    if (errors != other.errors) return false
    if (dependentKeys != other.dependentKeys) return false
    if (isFromCache != other.isFromCache) return false
    if (extensions != other.extensions) return false
    if (executionContext != other.executionContext) return false

    return true
  }

  override fun hashCode(): Int {
    var result = operation.hashCode()
    result = 31 * result + (data?.hashCode() ?: 0)
    result = 31 * result + (errors?.hashCode() ?: 0)
    result = 31 * result + dependentKeys.hashCode()
    result = 31 * result + isFromCache.hashCode()
    result = 31 * result + extensions.hashCode()
    return result
  }

  class Builder<T> internal constructor(internal val operation: Operation<*, *, *>) {
    internal var data: T? = null
    internal var errors: List<Error>? = null
    internal var dependentKeys: Set<String>? = null
    internal var fromCache: Boolean = false
    internal var extensions: Map<String, Any?>? = null
    internal var executionContext: ExecutionContext = ExecutionContext.Empty

    fun data(data: T?) = apply {
      this.data = data
    }

    fun errors(errors: List<Error>?) = apply {
      this.errors = errors
    }

    fun dependentKeys(dependentKeys: Set<String>?) = apply {
      this.dependentKeys = dependentKeys
    }

    fun fromCache(fromCache: Boolean) = apply {
      this.fromCache = fromCache
    }

    fun extensions(extensions: Map<String, Any?>?) = apply {
      this.extensions = extensions
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = executionContext
    }

    fun build(): Response<T> = Response(this)
  }

  companion object {

    @JvmStatic
    fun <T> builder(operation: Operation<*, *, *>): Builder<T> = Builder(operation)
  }
}
