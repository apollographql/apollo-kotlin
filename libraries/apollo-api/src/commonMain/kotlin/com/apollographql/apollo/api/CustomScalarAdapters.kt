package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime.
 *
 * For historical reasons, it also contains other context used when parsing response.
 * See https://github.com/apollographql/apollo-kotlin/pull/3813
 */
class CustomScalarAdapters private constructor(
    customScalarAdapters: Map<String, Adapter<*>>,
    /**
     * Operation variables used to determine whether the parser must parse @skip/@include fragments
     *
     */
    @JvmField
    val falseVariables: Set<String>?,
    /**
     * Identifiers used to determine whether the parser must parse deferred fragments
     */
    @JvmField
    val deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?,
    /**
     * Errors to use with @catch
     */
    @JvmField
    val errors: List<Error>?,
  ) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = customScalarAdapters

  fun <T : Any> adapterFor(name: String): Adapter<T>? {
    @Suppress("UNCHECKED_CAST")
    return adaptersMap[name] as Adapter<T>?
  }

  fun <T : Any> responseAdapterFor(customScalar: CustomScalarType): Adapter<T> {
    val registered = adaptersMap[customScalar.name]
    if (registered != null) {
      @Suppress("UNCHECKED_CAST")
      return registered as Adapter<T>
    }

    /**
     * Below are shortcuts to save the users a call to `registerCustomScalarAdapter`
     */
    @Suppress("UNCHECKED_CAST")
    return when (customScalar.className) {
      "com.apollographql.apollo.api.Upload" -> UploadAdapter
      "kotlin.String", "java.lang.String" -> StringAdapter
      "kotlin.Boolean", "java.lang.Boolean" -> BooleanAdapter
      "kotlin.Int", "java.lang.Int" -> IntAdapter
      "kotlin.Double", "java.lang.Double" -> DoubleAdapter
      "kotlin.Long", "java.lang.Long" -> LongAdapter
      "kotlin.Float", "java.lang.Float" -> FloatAdapter
      "kotlin.Any", "java.lang.Object" -> AnyAdapter
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a scalar Adapter?")
    } as Adapter<T>
  }

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = Builder().build()

    /**
     * Unsafe [CustomScalarAdapters]. They can only be used with `MapJsonReader` and `MapJsonWriter`. It will passthrough the values using
     * `MapJsonReader.nextValue` and `MapJsonWriter.value()`
     */
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v5_0_0)
    @Deprecated("PassThrough was only used internally by data builders and is removed in v5")
    val PassThrough: CustomScalarAdapters
      get() = error("GlobalBuilder removed in v5. Use BuilderScope(CustomScalarAdapters).")
  }

  @ApolloExperimental
  fun firstErrorStartingWith(path: List<Any>): Error? {
    return errors?.firstOrNull {
      it.path?.startsWith(path) == true
    }
  }

  private fun List<Any>.startsWith(responsePath: List<Any>): Boolean {
    // start at 1 to drop the `data.`
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

  /**
   * Returns a copy of this [CustomScalarAdapters] with the given parsing context. The adapters are
   * shared with this instance, making this much cheaper than going through [newBuilder], which
   * copies the whole adapters map.
   */
  internal fun copyWithParsingContext(
      falseVariables: Set<String>?,
      deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?,
      errors: List<Error>?,
  ): CustomScalarAdapters {
    if (falseVariables == null &&
        deferredFragmentIdentifiers == null &&
        errors == null &&
        this.falseVariables == null &&
        this.deferredFragmentIdentifiers == null &&
        this.errors == null
    ) {
      return this
    }
    return CustomScalarAdapters(
        adaptersMap,
        falseVariables,
        deferredFragmentIdentifiers,
        errors,
    )
  }

  fun newBuilder(): Builder {
    return Builder().addAll(this)
        .falseVariables(falseVariables)
        .deferredFragmentIdentifiers(deferredFragmentIdentifiers)
  }

  class Builder {
    private val adaptersMap: MutableMap<String, Adapter<*>> = mutableMapOf()
    private var falseVariables: Set<String>? = null
    private var deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null
    private var errors: List<Error>? = null

    fun falseVariables(falseVariables: Set<String>?) = apply {
      this.falseVariables = falseVariables
    }

    fun deferredFragmentIdentifiers(deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>?) = apply {
      this.deferredFragmentIdentifiers = deferredFragmentIdentifiers
    }

    fun errors(errors: List<Error>?) = apply {
      this.errors = errors
    }

    fun <T> add(
        name: String,
        adapter: Adapter<T>,
    ) = apply {
      adaptersMap[name] = adapter
    }

    fun <T> add(
        customScalarType: CustomScalarType,
        customScalarAdapter: Adapter<T>,
    ) = apply {
      adaptersMap[customScalarType.name] = customScalarAdapter
    }


    fun addAll(customScalarAdapters: CustomScalarAdapters) = apply {
      this.adaptersMap.putAll(customScalarAdapters.adaptersMap)
    }

    fun clear() {
      adaptersMap.clear()
    }

    fun build(): CustomScalarAdapters {
      return CustomScalarAdapters(
          adaptersMap,
          falseVariables,
          deferredFragmentIdentifiers,
          errors,
      )
    }
  }
}
