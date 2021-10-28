package com.apollographql.apollo3.api

import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 */
class CustomScalarAdapters() : ExecutionContext.Element {

  /**
   * @param customScalarAdapters a map from the GraphQL scalar name to the matching runtime [Adapter]
   */
  @Deprecated("Please use the default constructor and call .set() instead.  This will be removed in v3.0.0.")
  constructor(customScalarAdapters: Map<String, Adapter<*>>) : this() {
    this.customScalarAdapters.putAll(customScalarAdapters)
  }

  private val customScalarAdapters: MutableMap<String, Adapter<*>> = mutableMapOf()

  fun <T : Any> responseAdapterFor(customScalar: CustomScalarType): Adapter<T> {
    return when {
      customScalarAdapters[customScalar.name] != null -> {
        @Suppress("UNCHECKED_CAST")
        customScalarAdapters[customScalar.name] as Adapter<T>
      }
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        // Shortcut to save users a call to `registerCustomScalarAdapter`
        @Suppress("UNCHECKED_CAST")
        UploadAdapter as Adapter<T>
      }
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    }
  }

  /**
   * Registers the given [customScalarAdapter]
   *
   * @param customScalarType a generated [CustomScalarType] from the [Types] generated object
   * @param customScalarAdapter the [Adapter] to use for this custom scalar
   *
   * @return this [CustomScalarAdapters]
   */
  operator fun <T> set(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>): CustomScalarAdapters {
    customScalarAdapters[customScalarType.name] = customScalarAdapter
    return this
  }

  fun put(
      customScalarType: CustomScalarType,
      customScalarAdapter: Adapter<*>,
  ): Adapter<*>? = customScalarAdapters.put(customScalarType.name, customScalarAdapter)

  fun copy(): CustomScalarAdapters = CustomScalarAdapters().also {
    it.customScalarAdapters.putAll(customScalarAdapters)
  }

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = CustomScalarAdapters()
  }
}
