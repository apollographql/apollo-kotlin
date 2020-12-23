package com.apollographql.apollo.api

import com.apollographql.apollo.api.CustomTypeValue.*
import com.apollographql.apollo.api.CustomTypeValue.Companion.fromRawValue
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import kotlin.jvm.JvmField

class ScalarTypeAdapters(val customScalarTypeAdapters: Map<ScalarType, CustomScalarTypeAdapter<*>>) {

  private val customTypeAdapters = customScalarTypeAdapters.mapKeys { it.key.typeName() }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> adapterFor(scalarType: ScalarType): CustomScalarTypeAdapter<T> {
    /**
     * Look in user-registered adapters by scalar type name first
     */
    var customScalarTypeAdapter: CustomScalarTypeAdapter<*>? = customTypeAdapters[scalarType.typeName()]
    if (customScalarTypeAdapter == null) {
      /**
       * If none is found, provide a default adapter based on the implementation class name
       * This saves the user the hassle of registering a scalar adapter for mapping to widespread such as Long, Map, etc...
       * The ScalarType must still be declared in the Gradle plugin configuration.
       */
      customScalarTypeAdapter = DEFAULT_ADAPTERS[scalarType.className()]
    }
    return requireNotNull(customScalarTypeAdapter) {
      "Can't map GraphQL type: `${scalarType.typeName()}` to: `${scalarType.className()}`. Did you forget to add a custom type adapter?"
    } as CustomScalarTypeAdapter<T>
  }

  companion object {
    @JvmField
    val DEFAULT = ScalarTypeAdapters(emptyMap())

    private val DEFAULT_ADAPTERS = emptyMap<String, CustomScalarTypeAdapter<*>>() +
        createDefaultScalarTypeAdapter("java.lang.String", "kotlin.String") { value ->
          if (value is GraphQLJsonList || value is GraphQLJsonObject) {
            val buffer = Buffer()
            JsonWriter.of(buffer).use { writer ->
              Utils.writeToJson(value.value, writer)
            }
            buffer.readUtf8()
          } else {
            value.value.toString()
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Boolean", "kotlin.Boolean", "boolean") { value ->
          when (value) {
            is GraphQLBoolean -> value.value
            is GraphQLString -> value.value.toBoolean()
            else -> throw IllegalArgumentException("Can't decode: $value into Boolean")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Integer", "kotlin.Int", "int") { value ->
          when (value) {
            is GraphQLNumber -> value.value.toInt()
            is GraphQLString -> value.value.toInt()
            else -> throw IllegalArgumentException("Can't decode: $value into Integer")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Long", "kotlin.Long", "long") { value ->
          when (value) {
            is GraphQLNumber -> value.value.toLong()
            is GraphQLString -> value.value.toLong()
            else -> throw IllegalArgumentException("Can't decode: $value into Long")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Float", "kotlin.Float", "float") { value ->
          when (value) {
            is GraphQLNumber -> value.value.toFloat()
            is GraphQLString -> value.value.toFloat()
            else -> throw IllegalArgumentException("Can't decode: $value into Float")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Double", "kotlin.Double", "double") { value ->
          when (value) {
            is GraphQLNumber -> value.value.toDouble()
            is GraphQLString -> value.value.toDouble()
            else -> throw IllegalArgumentException("Can't decode: $value into Double")
          }
        } +
        mapOf("com.apollographql.apollo.api.FileUpload" to object : CustomScalarTypeAdapter<FileUpload> {
          override fun decode(value: CustomTypeValue<*>): FileUpload {
            // TODO: is there a valid use case for decoding a FileUpload or should we throw here?
            return FileUpload("", value.value?.toString() ?: "")
          }

          override fun encode(value: FileUpload): CustomTypeValue<*> {
            return GraphQLNull
          }
        }) +
        createDefaultScalarTypeAdapter("java.util.Map", "kotlin.collections.Map") { value ->
          if (value is GraphQLJsonObject) {
            value.value
          } else {
            throw IllegalArgumentException("Can't decode: $value into Map")
          }
        } +
        createDefaultScalarTypeAdapter("java.util.List", "kotlin.collections.List") { value ->
          if (value is GraphQLJsonList) {
            value.value
          } else {
            throw IllegalArgumentException("Can't decode: $value into List")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Object", "kotlin.Any") { value ->
          value.value!!
        }

    private fun createDefaultScalarTypeAdapter(
        vararg classNames: String,
        decode: (value: CustomTypeValue<*>) -> Any
    ): Map<String, CustomScalarTypeAdapter<*>> {
      val adapter = object : CustomScalarTypeAdapter<Any> {
        override fun decode(value: CustomTypeValue<*>): Any {
          return decode(value)
        }

        override fun encode(value: Any): CustomTypeValue<*> {
          return fromRawValue(value)
        }
      }
      return classNames.associate { it to adapter }
    }
  }
}
