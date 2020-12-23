package com.apollographql.apollo.api

import com.apollographql.apollo.api.JsonElement.*
import com.apollographql.apollo.api.JsonElement.Companion.fromRawValue
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
import com.apollographql.apollo.api.internal.json.use
import okio.Buffer
import kotlin.jvm.JvmField

class ScalarTypeAdapters(val customScalarTypeAdapters: Map<ScalarType, CustomScalarTypeAdapter<*>>) {

  private val CustomScalarTypeAdapters = customScalarTypeAdapters.mapKeys { it.key.graphqlName }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> adapterFor(scalarType: ScalarType): CustomScalarTypeAdapter<T> {
    /**
     * Look in user-registered adapters by scalar type name first
     */
    var customScalarTypeAdapter: CustomScalarTypeAdapter<*>? = CustomScalarTypeAdapters[scalarType.graphqlName]
    if (customScalarTypeAdapter == null) {
      /**
       * If none is found, provide a default adapter based on the implementation class name
       * This saves the user the hassle of registering a scalar adapter for mapping to widespread such as Long, Map, etc...
       * The ScalarType must still be declared in the Gradle plugin configuration.
       */
      customScalarTypeAdapter = DEFAULT_ADAPTERS[scalarType.className]
    }
    return requireNotNull(customScalarTypeAdapter) {
      "Can't map GraphQL type: `${scalarType.graphqlName}` to: `${scalarType.className}`. Did you forget to add a custom type adapter?"
    } as CustomScalarTypeAdapter<T>
  }

  companion object {
    @JvmField
    val DEFAULT = ScalarTypeAdapters(emptyMap())

    private val DEFAULT_ADAPTERS = emptyMap<String, CustomScalarTypeAdapter<*>>() +
        createDefaultScalarTypeAdapter("java.lang.String", "kotlin.String") { jsonElement ->
          if (jsonElement is JsonList || jsonElement is JsonObject) {
            val buffer = Buffer()
            JsonWriter.of(buffer).use { writer ->
              Utils.writeToJson(jsonElement.toRawValue(), writer)
            }
            buffer.readUtf8()
          } else {
            jsonElement.toRawValue().toString()
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Boolean", "kotlin.Boolean", "boolean") { jsonElement ->
          when (jsonElement) {
            is JsonBoolean -> jsonElement.value
            is JsonString -> jsonElement.value.toBoolean()
            else -> throw IllegalArgumentException("Can't decode: $jsonElement into Boolean")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Integer", "kotlin.Int", "int") { jsonElement ->
          when (jsonElement) {
            is JsonNumber -> jsonElement.value.toInt()
            is JsonString -> jsonElement.value.toInt()
            else -> throw IllegalArgumentException("Can't decode: $jsonElement into Integer")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Long", "kotlin.Long", "long") { jsonElement ->
          when (jsonElement) {
            is JsonNumber -> jsonElement.value.toLong()
            is JsonString -> jsonElement.value.toLong()
            else -> throw IllegalArgumentException("Can't decode: $jsonElement into Long")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Float", "kotlin.Float", "float") { jsonElement ->
          when (jsonElement) {
            is JsonNumber -> jsonElement.value.toFloat()
            is JsonString -> jsonElement.value.toFloat()
            else -> throw IllegalArgumentException("Can't decode: $jsonElement into Float")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Double", "kotlin.Double", "double") { jsonElement ->
          when (jsonElement) {
            is JsonNumber -> jsonElement.value.toDouble()
            is JsonString -> jsonElement.value.toDouble()
            else -> throw IllegalArgumentException("Can't decode: $jsonElement into Double")
          }
        } +
        mapOf("com.apollographql.apollo.api.FileUpload" to object : CustomScalarTypeAdapter<FileUpload> {
          override fun decode(jsonElement: JsonElement): FileUpload {
            // TODO: is there a valid use case for decoding a FileUpload or should we throw here?
            return FileUpload("", jsonElement.toRawValue()?.toString() ?: "")
          }

          override fun encode(value: FileUpload): JsonElement {
            return JsonNull
          }
        }) +
        createDefaultScalarTypeAdapter("java.util.Map", "kotlin.collections.Map") { jsonElement ->
          if (jsonElement is JsonObject) {
            jsonElement.value
          } else {
            throw IllegalArgumentException("Can't decode: $jsonElement into Map")
          }
        } +
        createDefaultScalarTypeAdapter("java.util.List", "kotlin.collections.List") { jsonElement ->
          if (jsonElement is JsonList) {
            jsonElement.value
          } else {
            throw IllegalArgumentException("Can't decode: $jsonElement into List")
          }
        } +
        createDefaultScalarTypeAdapter("java.lang.Object", "kotlin.Any") { jsonElement ->
          jsonElement.toRawValue()!!
        }

    private fun createDefaultScalarTypeAdapter(
        vararg classNames: String,
        decode: (jsonElement: JsonElement) -> Any
    ): Map<String, CustomScalarTypeAdapter<*>> {
      val adapter = object : CustomScalarTypeAdapter<Any> {
        override fun decode(jsonElement: JsonElement): Any {
          return decode(jsonElement)
        }

        override fun encode(value: Any): JsonElement {
          return fromRawValue(value)
        }
      }
      return classNames.associate { it to adapter }
    }
  }
}
