package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException
import kotlin.jvm.JvmField

/**
 * A [CompositeAdapter] is responsible for adapting Kotlin-generated GraphQL composite types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * This class is implemented by the generated code, it shouldn't be used directly.
 */
interface CompositeAdapter<T> {
  @Throws(IOException::class)
  fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): T

  @Throws(IOException::class)
  fun toJson(writer: JsonWriter, value: T, adapterContext: CompositeAdapterContext)
}

class CompositeAdapterContext private constructor(
    @JvmField
    val customScalarAdapters: CustomScalarAdapters,

    @JvmField
    val falseVariables: Set<String>,

    @JvmField
    val deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?,

    private var errors: List<Error>,
) {


  @ApolloExperimental
  fun errorsForPath(path: List<Any>) = errorsForPath(path, errors)

  class Builder {
    private var customScalarAdapters: CustomScalarAdapters? = null
    private var falseVariables: Set<String>? = null
    private var deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null
    private var errors: List<Error>? = null

    fun customScalarAdapters(customScalarAdapters: CustomScalarAdapters) = apply {
      this.customScalarAdapters = customScalarAdapters
    }

    fun falseVariables(falseVariables: Set<String>?) = apply {
      this.falseVariables = falseVariables
    }

    fun deferredFragmentIdentifiers(deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?) = apply {
      this.deferredFragmentIdentifiers = deferredFragmentIdentifiers
    }

    fun errors(errors: List<Error>) = apply {
      this.errors = errors
    }

    fun build(): CompositeAdapterContext {
      return CompositeAdapterContext(
          customScalarAdapters ?: CustomScalarAdapters.Empty,
          falseVariables ?: emptySet(),
          deferredFragmentIdentifiers,
          errors ?: emptyList()
      )
    }

  }
}

fun <T> CompositeAdapter<T>.toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
  toJson(writer, value, CompositeAdapterContext.Builder().customScalarAdapters(customScalarAdapters).build())
}

fun <T> CompositeAdapter<T>.fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
  return fromJson(reader, CompositeAdapterContext.Builder().customScalarAdapters(customScalarAdapters).build())
}

private fun List<Any>.startsWith(responsePath: List<Any>): Boolean {
  for (i in 1.until(responsePath.size)) {
    if (i - 1 >= this.size) {
      return false
    }
    if (responsePath[i] != this[i - 1]) {
      return false
    }
  }
  return true
}

private fun errorsForPath(responsePath: List<Any>, errors: List<Error>): List<Error> {
  // XXX: optimize
  return errors.filter {
    it.path?.startsWith(responsePath) ?: false
  }
}