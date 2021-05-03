package com.apollographql.apollo3.api

/**
 * A cache of [Adapter] so that they are only built once for each query/fragments
 *
 * @param customScalarResponseAdapters a map from [CustomScalar] to the matching runtime [Adapter]
 */
class CustomScalarAdpaters(val customScalarResponseAdapters: Map<String, Adapter<*>>): ClientContext(Key) {

  fun <T : Any> responseAdapterFor(customScalar: CustomScalar): Adapter<T> {
    return when {
      customScalarResponseAdapters[customScalar.name] != null -> {
        customScalarResponseAdapters[customScalar.name] as Adapter<T>
      }
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        // Shortcut to save users a call to `registerCustomScalarAdapter`
        UploadAdapter as Adapter<T>
      }
      else -> error("Can't map GraphQL type: `${customScalar.name}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    }

  }

  companion object Key: ExecutionContext.Key<CustomScalarAdpaters> {
    val DEFAULT = CustomScalarAdpaters(emptyMap())
  }
}
