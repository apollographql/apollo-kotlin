package com.apollographql.apollo3.api

import kotlin.jvm.JvmField

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 *
 * @param customScalarAdapters a map from the GraphQL scalar name to the matching runtime [Adapter]
 */
class CustomScalarAdapters(val customScalarAdapters: Map<String, Adapter<*>>): ExecutionContext.Element {

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

  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key: ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    @JvmField
    val Empty = CustomScalarAdapters(emptyMap())
  }
}
