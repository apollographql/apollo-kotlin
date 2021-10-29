package com.apollographql.apollo3.api

import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 */
class CustomScalarAdapters
@Deprecated("Please use CustomScalarAdapters.Builder instead.  This will be removed in v3.0.0.")
/* private */ constructor(
    customScalarAdapters: Map<String, Adapter<*>>,
) : ExecutionContext.Element {

  private val adaptersMap: Map<String, Adapter<*>> = customScalarAdapters

  fun <T : Any> responseAdapterFor(customScalar: CustomScalarType): Adapter<T> {
    return when {
      adaptersMap[customScalar.name] != null -> {
        @Suppress("UNCHECKED_CAST")
        adaptersMap[customScalar.name] as Adapter<T>
      }
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        // Shortcut to save users a call to `registerCustomScalarAdapter`
        @Suppress("UNCHECKED_CAST")
        UploadAdapter as Adapter<T>
      }
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    }
  }

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = CustomScalarAdapters(emptyMap())
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

    fun build() = CustomScalarAdapters(adaptersMap)
  }
}
