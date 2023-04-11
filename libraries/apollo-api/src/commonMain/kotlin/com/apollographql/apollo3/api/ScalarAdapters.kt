package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve scalar adapters at runtime
 */
class ScalarAdapters private constructor(
    scalarAdapters: Map<String, Adapter<*>>,
    // We piggyback ScalarAdapters to pass around a context which is used in the Adapters at parse time.
    // This is currently used for @skip/@include and @defer.
    // Ideally it should be passed as its own parameter, but we're avoiding a breaking change.
    // See https://github.com/apollographql/apollo-kotlin/pull/3813
    val adapterContext: AdapterContext,
    private val unsafe: Boolean,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = scalarAdapters

  fun <T : Any> responseAdapterFor(scalar: ScalarType): Adapter<T> {
    @Suppress("UNCHECKED_CAST")
    return when {
      adaptersMap[scalar.name] != null -> {
        adaptersMap[scalar.name]
      }
      /**
       * Below are shortcuts to save the users a call to `registerScalarAdapter`
       */
      scalar.className == "com.apollographql.apollo3.api.Upload" -> {
        UploadAdapter
      }

      scalar.className in listOf("kotlin.String", "java.lang.String") -> {
        StringAdapter
      }

      scalar.className in listOf("kotlin.Boolean", "java.lang.Boolean") -> {
        BooleanAdapter
      }

      scalar.className in listOf("kotlin.Int", "java.lang.Int") -> {
        IntAdapter
      }

      scalar.className in listOf("kotlin.Double", "java.lang.Double") -> {
        DoubleAdapter
      }

      scalar.className in listOf("kotlin.Long", "java.lang.Long") -> {
        LongAdapter
      }

      scalar.className in listOf("kotlin.Float", "java.lang.Float") -> {
        FloatAdapter
      }

      scalar.className in listOf("kotlin.Any", "java.lang.Object") -> {
        AnyAdapter
      }

      unsafe -> PassThroughAdapter()
      else -> error("Can't map GraphQL type: `${scalar.name}` to: `${scalar.className}`. Did you forget to add a ScalarAdapter?")
    } as Adapter<T>
  }

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<ScalarAdapters> {
    /**
     * An empty [ScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = Builder().build()

    /**
     * Unsafe [ScalarAdapters]. They can only be used with `MapJsonReader` and `MapJsonWriter`. It will passthrough the values using
     * `MapJsonReader.nextValue` and `MapJsonWriter.value()`
     */
    @JvmField
    @ApolloExperimental
    val PassThrough = Builder().unsafe(true).build()
  }

  fun newBuilder() = Builder().addAll(this)

  class Builder {
    private val adaptersMap: MutableMap<String, Adapter<*>> = mutableMapOf()
    private var adapterContext: AdapterContext = AdapterContext.Builder().build()
    private var unsafe = false

    fun <T> add(
        scalarType: ScalarType,
        scalarAdapter: ScalarAdapter<T>,
    ) = apply {
      adaptersMap[scalarType.name] = ScalarAdapterToApolloAdapter(scalarAdapter)
    }

    fun addAll(scalarAdapters: ScalarAdapters) = apply {
      this.adaptersMap.putAll(scalarAdapters.adaptersMap)
    }

    @ApolloExperimental
    fun unsafe(unsafe: Boolean) = apply {
      this.unsafe = unsafe
    }

    fun clear() {
      adaptersMap.clear()
    }

    fun build() = ScalarAdapters(adaptersMap, adapterContext, unsafe)

    fun adapterContext(adapterContext: AdapterContext): Builder = apply {
      this.adapterContext = adapterContext
    }
  }
}
