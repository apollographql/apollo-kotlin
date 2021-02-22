package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.ResponseAdapter
import com.apollographql.apollo3.api.internal.UploadResponseAdapter
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.api.internal.json.Utils.writeToJson
import kotlin.reflect.KClass

/**
 * A cache of [ResponseAdapter] so that they are only built once for each query/fragments
 *
 * @param customScalarAdapters a map from [CustomScalar] to the matching runtime [CustomScalarAdapter]
 */
class ResponseAdapterCache(val customScalarAdapters: Map<CustomScalar, CustomScalarAdapter<*>>) {

  private val adapterByGraphQLName = customScalarAdapters.mapKeys { it.key.graphqlName }

  private val adapterByClass = ThreadSafeMap<KClass<*>, ResponseAdapter<*>>()
  private val variableAdapterByClass = ThreadSafeMap<KClass<*>, ResponseAdapter<*>>()

  private val responseAdapterByGraphQLName = mutableMapOf<String, ResponseAdapter<*>>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> adapterFor(customScalar: CustomScalar): CustomScalarAdapter<T> {
    /**
     * Look in user-registered adapters by scalar type name first
     */
    var customScalarAdapter: CustomScalarAdapter<*>? = adapterByGraphQLName[customScalar.graphqlName]
    if (customScalarAdapter == null) {
      /**
       * If none is found, provide a default adapter based on the implementation class name
       * This saves the user the hassle of registering a scalar adapter for mapping to widespread such as Long, Map, etc...
       * The ScalarType must still be declared in the Gradle plugin configuration (except for Any that will fallback here all the time)
       *
       * TODO: we could determine during codegen if we're going to fallback to Any and remove this hook
       */
      customScalarAdapter = adapterByClassName[customScalar.className]
    }
    return requireNotNull(customScalarAdapter) {
      "Can't map GraphQL type: `${customScalar.graphqlName}` to: `${customScalar.className}`. Did you forget to add a CustomScalarAdapter?"
    } as CustomScalarAdapter<T>
  }
  fun registerCustomScalarResponseAdapter(scalar: String, adapter: ResponseAdapter<*>) {
    responseAdapterByGraphQLName[scalar] = adapter
  }
  fun <T : Any> responseAdapterFor(customScalar: CustomScalar): ResponseAdapter<T> {
    if (responseAdapterByGraphQLName[customScalar.graphqlName] != null) {
      return responseAdapterByGraphQLName[customScalar.graphqlName] as ResponseAdapter<T>
    }
    if (customScalar.className == "com.apollographql.apollo3.api.Upload") {
      return UploadResponseAdapter as ResponseAdapter<T>
    }
    return CustomResponseAdapter(adapterFor(customScalar))
  }

  @Suppress("UNCHECKED_CAST")
  fun <D> getAdapterFor(klass: KClass<*>, defaultValue: () -> ResponseAdapter<D>): ResponseAdapter<D> {
    return adapterByClass.getOrPut(klass, defaultValue) as ResponseAdapter<D>
  }

  @Suppress("UNCHECKED_CAST")
  fun <D> getVariablesAdapterFor(klass: KClass<*>, defaultValue: () -> ResponseAdapter<D>): ResponseAdapter<D> {
    return variableAdapterByClass.getOrPut(klass, defaultValue) as ResponseAdapter<D>
  }


  class CustomResponseAdapter<T: Any>(private val wrappedAdapter: CustomScalarAdapter<T>) : ResponseAdapter<T> {
    override fun fromResponse(reader: JsonReader): T {
      return wrappedAdapter.decode(JsonElement.fromRawValue(reader.readRecursively()))
    }

    override fun toResponse(writer: JsonWriter, value: T) {
      writeToJson(wrappedAdapter.encode(value).toRawValue(), writer)
    }
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

    private val adapterByClassName = mapOf(
        "java.lang.String" to BuiltinCustomScalarAdapters.STRING_ADAPTER,
        "kotlin.String" to  BuiltinCustomScalarAdapters.STRING_ADAPTER,

        "java.lang.Boolean" to BuiltinCustomScalarAdapters.BOOLEAN_ADAPTER,
        "boolean" to  BuiltinCustomScalarAdapters.BOOLEAN_ADAPTER,
        "kotlin.Boolean" to  BuiltinCustomScalarAdapters.BOOLEAN_ADAPTER,

        "java.lang.Integer" to BuiltinCustomScalarAdapters.INT_ADAPTER,
        "int" to BuiltinCustomScalarAdapters.INT_ADAPTER,
        "kotlin.Int" to  BuiltinCustomScalarAdapters.INT_ADAPTER,

        "java.lang.Long" to BuiltinCustomScalarAdapters.LONG_ADAPTER,
        "long" to BuiltinCustomScalarAdapters.LONG_ADAPTER,
        "kotlin.Long" to  BuiltinCustomScalarAdapters.LONG_ADAPTER,

        "java.lang.Float" to BuiltinCustomScalarAdapters.FLOAT_ADAPTER,
        "float" to BuiltinCustomScalarAdapters.FLOAT_ADAPTER,
        "kotlin.Float" to  BuiltinCustomScalarAdapters.FLOAT_ADAPTER,

        "java.lang.Double" to BuiltinCustomScalarAdapters.DOUBLE_ADAPTER,
        "double" to BuiltinCustomScalarAdapters.DOUBLE_ADAPTER,
        "kotlin.Double" to  BuiltinCustomScalarAdapters.DOUBLE_ADAPTER,

        "java.util.Map" to BuiltinCustomScalarAdapters.MAP_ADAPTER,
        "kotlin.collections.Map" to  BuiltinCustomScalarAdapters.MAP_ADAPTER,

        "java.util.List" to BuiltinCustomScalarAdapters.LIST_ADAPTER,
        "kotlin.collections.List" to  BuiltinCustomScalarAdapters.LIST_ADAPTER,

        "java.lang.Object" to  BuiltinCustomScalarAdapters.FALLBACK_ADAPTER,
        "kotlin.Any" to  BuiltinCustomScalarAdapters.FALLBACK_ADAPTER,
    )
  }
}
