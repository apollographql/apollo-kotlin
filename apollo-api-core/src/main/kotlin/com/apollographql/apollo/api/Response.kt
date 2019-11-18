package com.apollographql.apollo.api

import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableSet

/**
 * Represents either a successful or failed response received from the GraphQL server.
 */
class Response<T> internal constructor(builder: Builder<T>) {
  private val operation: Operation<*, *, *>
  private val data: T?
  private val errors: List<Error>
  private val dependentKeys: Set<String>
  private val fromCache: Boolean

  init {
    operation = builder.operation
    data = builder.data
    errors = builder.errors?.let { unmodifiableList(it) }.orEmpty()
    dependentKeys = builder.dependentKeys?.let { unmodifiableSet(it) }.orEmpty()
    fromCache = builder.fromCache
  }

  fun operation(): Operation<*, *, *> {
    return operation
  }

  fun data(): T? = data

  fun errors(): List<Error> = errors

  fun dependentKeys(): Set<String> {
    return dependentKeys
  }

  fun hasErrors(): Boolean = errors.isNotEmpty()

  fun fromCache(): Boolean {
    return fromCache
  }

  fun toBuilder(): Builder<T> = Builder<T>(operation)
      .data(data)
      .errors(errors)
      .dependentKeys(dependentKeys)
      .fromCache(fromCache)

  class Builder<T> internal constructor(internal val operation: Operation<*, *, *>) {
    internal var data: T? = null
    internal var errors: List<Error>? = null
    internal var dependentKeys: Set<String>? = null
    internal var fromCache: Boolean = false

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

    fun build(): Response<T> = Response(this)
  }

  companion object {

    @JvmStatic
    fun <T> builder(operation: Operation<*, *, *>): Builder<T> = Builder(operation)
  }
}
