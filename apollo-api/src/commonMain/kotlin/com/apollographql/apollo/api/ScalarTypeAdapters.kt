package com.apollographql.apollo.api

import com.apollographql.apollo.api.JsonElement.*
import kotlin.jvm.JvmField

class ScalarTypeAdapters(val customScalarAdapters: Map<ScalarType, CustomScalarAdapter<*>>) {

  private val adapterByGraphQLName = customScalarAdapters.mapKeys { it.key.graphqlName }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> adapterFor(scalarType: ScalarType): CustomScalarAdapter<T> {
    /**
     * Look in user-registered adapters by scalar type name first
     */
    var customScalarAdapter: CustomScalarAdapter<*>? = adapterByGraphQLName[scalarType.graphqlName]
    if (customScalarAdapter == null) {
      /**
       * If none is found, provide a default adapter based on the implementation class name
       * This saves the user the hassle of registering a scalar adapter for mapping to widespread such as Long, Map, etc...
       * The ScalarType must still be declared in the Gradle plugin configuration.
       */
      customScalarAdapter = adapterByClassName[scalarType.className]
    }
    return requireNotNull(customScalarAdapter) {
      "Can't map GraphQL type: `${scalarType.graphqlName}` to: `${scalarType.className}`. Did you forget to add a CustomScalarAdapter?"
    } as CustomScalarAdapter<T>
  }

  companion object {
    val DEFAULT = ScalarTypeAdapters(emptyMap())

    private val adapterByClassName = mapOf(
        "java.lang.String" to BuiltinScalarTypeAdapters.STRING_ADAPTER,
        "kotlin.String" to  BuiltinScalarTypeAdapters.STRING_ADAPTER,

        "java.lang.Boolean" to BuiltinScalarTypeAdapters.BOOLEAN_ADAPTER,
        "boolean" to  BuiltinScalarTypeAdapters.BOOLEAN_ADAPTER,
        "kotlin.Boolean" to  BuiltinScalarTypeAdapters.BOOLEAN_ADAPTER,

        "java.lang.Integer" to BuiltinScalarTypeAdapters.INT_ADAPTER,
        "int" to BuiltinScalarTypeAdapters.INT_ADAPTER,
        "kotlin.Int" to  BuiltinScalarTypeAdapters.INT_ADAPTER,

        "java.lang.Long" to BuiltinScalarTypeAdapters.LONG_ADAPTER,
        "long" to BuiltinScalarTypeAdapters.LONG_ADAPTER,
        "kotlin.Long" to  BuiltinScalarTypeAdapters.LONG_ADAPTER,

        "java.lang.Float" to BuiltinScalarTypeAdapters.FLOAT_ADAPTER,
        "float" to BuiltinScalarTypeAdapters.FLOAT_ADAPTER,
        "kotlin.Float" to  BuiltinScalarTypeAdapters.FLOAT_ADAPTER,

        "java.lang.Double" to BuiltinScalarTypeAdapters.DOUBLE_ADAPTER,
        "double" to BuiltinScalarTypeAdapters.DOUBLE_ADAPTER,
        "kotlin.Double" to  BuiltinScalarTypeAdapters.DOUBLE_ADAPTER,

        "java.util.Map" to BuiltinScalarTypeAdapters.MAP_ADAPTER,
        "kotlin.collections.Map" to  BuiltinScalarTypeAdapters.MAP_ADAPTER,

        "java.util.List" to BuiltinScalarTypeAdapters.LIST_ADAPTER,
        "kotlin.collections.List" to  BuiltinScalarTypeAdapters.LIST_ADAPTER,

        "com.apollographql.apollo.api.FileUpload" to  BuiltinScalarTypeAdapters.FILE_UPLOAD_ADAPTER,

        "java.lang.Object" to  BuiltinScalarTypeAdapters.FALLBACK_ADAPTER,
        "kotlin.Any" to  BuiltinScalarTypeAdapters.FALLBACK_ADAPTER,
    )
  }
}
