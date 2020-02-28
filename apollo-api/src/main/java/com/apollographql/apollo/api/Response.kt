package com.apollographql.apollo.api

/**
 * Represents either a successful or failed response received from the GraphQL server.
 */
data class Response<T>(
    /**
     * GraphQL operation this response represents of
     */
    @JvmField
    val operation: Operation<*, *, *>,

    /**
     * Parsed response of GraphQL [operation] execution.
     * Can be `null` in case if [operation] execution failed.
     */
    @JvmField
    val data: T?,

    /**
     * GraphQL [operation] execution errors returned by the server to let client know that something has gone wrong.
     */
    @JvmField
    val errors: List<Error>? = null,

    /**
     * Set of request object keys to identify cache records to invalidate.
     * Used by normalized cache implementation.
     */
    @JvmField
    val dependentKeys: Set<String> = emptySet(),

    /**
     * Indicates if response is resolved from the cache.
     */
    @JvmField
    val fromCache: Boolean,

    /**
     * Extensions of GraphQL protocol, arbitrary map of key [String] / value [Any] sent by server along with the response.
     */
    @JvmField
    val extensions: Map<String, Any?> = emptyMap()
) {

  constructor(builder: Builder<T>) : this(
      operation = builder.operation,
      data = builder.data,
      errors = builder.errors,
      dependentKeys = builder.dependentKeys.orEmpty(),
      fromCache = builder.fromCache,
      extensions = builder.extensions.orEmpty()
  )

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "operation"))
  fun operation(): Operation<*, *, *> {
    return operation
  }

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "data"))
  fun data(): T? = data

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "errors"))
  fun errors(): List<Error>? = errors

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "dependentKeys"))
  fun dependentKeys(): Set<String> {
    return dependentKeys
  }

  fun hasErrors(): Boolean = !errors.isNullOrEmpty()

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "fromCache"))
  fun fromCache(): Boolean {
    return fromCache
  }

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "extensions"))
  fun extensions(): Map<String, Any?> {
    return extensions
  }

  fun toBuilder(): Builder<T> = Builder<T>(operation)
      .data(data)
      .errors(errors)
      .dependentKeys(dependentKeys)
      .fromCache(fromCache)
      .extensions(extensions)

  class Builder<T> internal constructor(internal val operation: Operation<*, *, *>) {
    internal var data: T? = null
    internal var errors: List<Error>? = null
    internal var dependentKeys: Set<String>? = null
    internal var fromCache: Boolean = false
    internal var extensions: Map<String, Any?>? = null

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

    fun build(): Response<T> = Response(this)
  }

  companion object {

    @JvmStatic
    fun <T> builder(operation: Operation<*, *, *>): Builder<T> = Builder(operation)
  }
}
