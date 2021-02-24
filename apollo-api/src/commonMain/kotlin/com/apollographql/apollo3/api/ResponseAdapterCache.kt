package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.ThreadSafeMap
import kotlin.reflect.KClass

/**
 * A cache of [ResponseAdapter] so that they are only built once for each query/fragments
 *
 * @param customScalarResponseAdapters a map from [CustomScalar] to the matching runtime [ResponseAdapter]
 */
class ResponseAdapterCache(val customScalarResponseAdapters: Map<CustomScalar, ResponseAdapter<*>>) {

  private val adapterByClass = ThreadSafeMap<KClass<*>, ResponseAdapter<*>>()
  private val variableAdapterByClass = ThreadSafeMap<KClass<*>, ResponseAdapter<*>>()

  fun <T : Any> responseAdapterFor(customScalar: CustomScalar): ResponseAdapter<T> {
    return when {
      customScalarResponseAdapters[customScalar] != null -> {
        customScalarResponseAdapters[customScalar] as ResponseAdapter<T>
      }
      customScalar.className == "com.apollographql.apollo3.api.Upload" -> {
        UploadResponseAdapter as ResponseAdapter<T>
      }
      customScalar.className == "kotlin.Any" -> {
        // Fallback adapter
        // This could be determined at compile time to avoid a lookup
        AnyResponseAdapter as ResponseAdapter<T>
      }
      else -> error("Can't map GraphQL type: `${customScalar.graphqlName}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?")
    }

  }

  @Suppress("UNCHECKED_CAST")
  fun <D> getAdapterFor(klass: KClass<*>, defaultValue: () -> ResponseAdapter<D>): ResponseAdapter<D> {
    return adapterByClass.getOrPut(klass, defaultValue) as ResponseAdapter<D>
  }

  @Suppress("UNCHECKED_CAST")
  fun <D> getVariablesAdapterFor(klass: KClass<*>, defaultValue: () -> ResponseAdapter<D>): ResponseAdapter<D> {
    return variableAdapterByClass.getOrPut(klass, defaultValue) as ResponseAdapter<D>
  }

  /**
   * releases resources associated with this [ResponseAdapterCache].
   *
   * Use it on native to release the [kotlinx.cinterop.StableRef]
   */
  fun dispose() {
    variableAdapterByClass.dispose()
    adapterByClass.dispose()
  }

  companion object {
    val DEFAULT = ResponseAdapterCache(emptyMap())
  }
}
