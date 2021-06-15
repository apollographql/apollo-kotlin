package com.apollographql.apollo3.api

/**
 * A wrapper around a Map<String, [Adapter]> used to retrieve custom scalar adapters at runtime
 *
 * @param customScalarAdapters a map from the GraphQL scalar name to the matching runtime [Adapter]
 */
class CustomScalarAdapters(val customScalarAdapters: Map<String, Adapter<*>>): ClientContext(Key) {

  fun <T : Any> responseAdapterFor(customScalar: CustomScalarType): Adapter<T> {
    return when {
      customScalarAdapters[customScalar.name] != null -> {
        customScalarAdapters[customScalar.name] as Adapter<T>
      }
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        // Shortcut to save users a call to `registerCustomScalarAdapter`
        UploadAdapter as Adapter<T>
      }
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    }

  }

  companion object Key: ExecutionContext.Key<CustomScalarAdapters> {
    /**
     * An empty [CustomScalarAdapters]. If the models were generated with some custom scalars, parsing will fail
     */
    val Empty = CustomScalarAdapters(emptyMap())
  }
}
