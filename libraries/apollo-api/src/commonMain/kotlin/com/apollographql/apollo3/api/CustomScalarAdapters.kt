package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 */
class CustomScalarAdapters private constructor(
    customScalarAdapters: Map<String, Adapter<*>>,
    private val unsafe: Boolean,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = customScalarAdapters

  fun <T: Any> adapterFor(name: String): Adapter<T>? {
    @Suppress("UNCHECKED_CAST")
    return adaptersMap[name] as Adapter<T>?
  }

  fun <T : Any> responseAdapterFor(customScalar: CustomScalarType): Adapter<T> {
    @Suppress("UNCHECKED_CAST")
    return when {
      adaptersMap[customScalar.name] != null -> {
        adaptersMap[customScalar.name]
      }
      /**
       * Below are shortcuts to save the users a call to `registerCustomScalarAdapter`
       */
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        UploadAdapter
      }

      customScalar.className in listOf("kotlin.String", "java.lang.String") -> {
        StringAdapter
      }

      customScalar.className in listOf("kotlin.Boolean", "java.lang.Boolean") -> {
        BooleanAdapter
      }

      customScalar.className in listOf("kotlin.Int", "java.lang.Int") -> {
        IntAdapter
      }

      customScalar.className in listOf("kotlin.Double", "java.lang.Double") -> {
        DoubleAdapter
      }

      customScalar.className in listOf("kotlin.Long", "java.lang.Long") -> {
        LongAdapter
      }

      customScalar.className in listOf("kotlin.Float", "java.lang.Float") -> {
        FloatAdapter
      }

      customScalar.className in listOf("kotlin.Any", "java.lang.Object") -> {
        AnyAdapter
      }

      unsafe -> PassThroughAdapter()
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
    @JvmField
    @ApolloExperimental
    val PassThrough = Builder().unsafe(true).build()
  }

  fun newBuilder() = Builder().addAll(this)

  class Builder {
    private val adaptersMap: MutableMap<String, Adapter<*>> = mutableMapOf()
    private var unsafe = false

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

    @ApolloExperimental
    fun unsafe(unsafe: Boolean) = apply {
      this.unsafe = unsafe
    }

    fun clear() {
      adaptersMap.clear()
    }

    fun build() = CustomScalarAdapters(adaptersMap, unsafe)
  }
}
