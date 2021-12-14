package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.internal.Version2CustomTypeAdapterToAdapter
import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 */
class CustomScalarAdapters
private constructor(
    customScalarAdapters: Map<String, Adapter<*>>,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = customScalarAdapters

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
      customScalar.className in listOf("kotlin.Int", "java.lang.Int")  -> {
        IntAdapter
      }
      customScalar.className in listOf("kotlin.Double", "java.lang.Double") -> {
        DoubleAdapter
      }
      customScalar.className in listOf("kotlin.Long", "java.lang.Long") -> {
        LongAdapter
      }
      customScalar.className in listOf("kotlin.Float", "java.lang.Float")  -> {
        FloatAdapter
      }
      customScalar.className in listOf("kotlin.Any", "java.lang.Object")  -> {
        AnyAdapter
      }
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
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
  }

  fun newBuilder() = Builder().addAll(this)

  class Builder {
    private val adaptersMap: MutableMap<String, Adapter<*>> = mutableMapOf()

    fun <T> add(
        customScalarType: CustomScalarType,
        customScalarAdapter: Adapter<T>,
    ) = apply {
      adaptersMap[customScalarType.name] = customScalarAdapter
    }

    @Suppress("DEPRECATION")
    @OptIn(ApolloInternal::class)
    @Deprecated("Used for backward compatibility with 2.x")
    fun <T> add(
        customScalarType: CustomScalarType,
        customTypeAdapter: CustomTypeAdapter<T>,
    ) = apply {
      adaptersMap[customScalarType.name] = Version2CustomTypeAdapterToAdapter(customTypeAdapter)
    }

    fun addAll(customScalarAdapters: CustomScalarAdapters) = apply {
      this.adaptersMap.putAll(customScalarAdapters.adaptersMap)
    }

    fun clear() {
      adaptersMap.clear()
    }

    @Suppress("DEPRECATION")
    fun build() = CustomScalarAdapters(adaptersMap)
  }
}
