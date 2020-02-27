package com.apollographql.apollo.api

import com.apollographql.apollo.api.CustomTypeValue.*
import com.apollographql.apollo.api.CustomTypeValue.Companion.fromRawValue
import java.io.File

class ScalarTypeAdapters(customAdapters: Map<ScalarType, CustomTypeAdapter<*>>) {
  private val customAdapters = customAdapters.mapKeys { it.key.typeName() }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> adapterFor(scalarType: ScalarType): CustomTypeAdapter<T> {
    var customTypeAdapter: CustomTypeAdapter<*>? = customAdapters[scalarType.typeName()]
    if (customTypeAdapter == null) {
      customTypeAdapter = DEFAULT_ADAPTERS[scalarType.javaType()]
    }
    return requireNotNull(customTypeAdapter) {
      "Can't map GraphQL type: `${scalarType.typeName()}` to: `${scalarType.javaType()}`. Did you forget to add a custom type adapter?"
    } as CustomTypeAdapter<T>
  }

  private abstract class DefaultCustomTypeAdapter<T : Any> : CustomTypeAdapter<T> {
    override fun encode(value: T): CustomTypeValue<*> {
      return fromRawValue(value)
    }
  }

  companion object {
    @JvmStatic
    val DEFAULT = ScalarTypeAdapters(emptyMap())

    private val DEFAULT_ADAPTERS = mapOf(
        String::class.java to object : DefaultCustomTypeAdapter<String>() {
          override fun decode(value: CustomTypeValue<*>): String {
            return value.value.toString()
          }
        },
        java.lang.Boolean::class.java to object : DefaultCustomTypeAdapter<Boolean>() {
          override fun decode(value: CustomTypeValue<*>): Boolean {
            return when (value) {
              is GraphQLBoolean -> value.value
              is GraphQLString -> java.lang.Boolean.parseBoolean(value.value)
              else -> throw IllegalArgumentException("Can't decode: $value into Boolean")
            }
          }
        },
        java.lang.Integer::class.java to object : DefaultCustomTypeAdapter<Int>() {
          override fun decode(value: CustomTypeValue<*>): Int {
            return when (value) {
              is GraphQLNumber -> value.value.toInt()
              is GraphQLString -> value.value.toInt()
              else -> throw IllegalArgumentException("Can't decode: $value into Integer")
            }
          }
        },
        java.lang.Long::class.java to object : DefaultCustomTypeAdapter<Long>() {
          override fun decode(value: CustomTypeValue<*>): Long {
            return when (value) {
              is GraphQLNumber -> value.value.toLong()
              is GraphQLString -> value.value.toLong()
              else -> throw IllegalArgumentException("Can't decode: $value into Long")
            }
          }
        },
        java.lang.Float::class.java to object : DefaultCustomTypeAdapter<Float>() {
          override fun decode(value: CustomTypeValue<*>): Float {
            return when (value) {
              is GraphQLNumber -> value.value.toFloat()
              is GraphQLString -> value.value.toFloat()
              else -> throw IllegalArgumentException("Can't decode: $value into Float")
            }
          }
        },
        java.lang.Double::class.java to object : DefaultCustomTypeAdapter<Double>() {
          override fun decode(value: CustomTypeValue<*>): Double {
            return when (value) {
              is GraphQLNumber -> value.value.toDouble()
              is GraphQLString -> value.value.toDouble()
              else -> throw IllegalArgumentException("Can't decode: $value into Double")
            }
          }
        },
        Boolean::class.java to object : DefaultCustomTypeAdapter<Boolean>() {
          override fun decode(value: CustomTypeValue<*>): Boolean {
            return when (value) {
              is GraphQLBoolean -> value.value
              is GraphQLString -> java.lang.Boolean.parseBoolean(value.value)
              else -> throw IllegalArgumentException("Can't decode: $value into Boolean")
            }
          }
        },
        Int::class.java to object : DefaultCustomTypeAdapter<Int>() {
          override fun decode(value: CustomTypeValue<*>): Int {
            return when (value) {
              is GraphQLNumber -> value.value.toInt()
              is GraphQLString -> value.value.toInt()
              else -> throw IllegalArgumentException("Can't decode: $value into Integer")
            }
          }
        },
        Long::class.java to object : DefaultCustomTypeAdapter<Long>() {
          override fun decode(value: CustomTypeValue<*>): Long {
            return when (value) {
              is GraphQLNumber -> value.value.toLong()
              is GraphQLString -> value.value.toLong()
              else -> throw IllegalArgumentException("Can't decode: $value into Long")
            }
          }
        },
        Float::class.java to object : DefaultCustomTypeAdapter<Float>() {
          override fun decode(value: CustomTypeValue<*>): Float {
            return when (value) {
              is GraphQLNumber -> value.value.toFloat()
              is GraphQLString -> value.value.toFloat()
              else -> throw IllegalArgumentException("Can't decode: $value into Float")
            }
          }
        },
        Double::class.java to object : DefaultCustomTypeAdapter<Double>() {
          override fun decode(value: CustomTypeValue<*>): Double {
            return when (value) {
              is GraphQLNumber -> value.value.toDouble()
              is GraphQLString -> value.value.toDouble()
              else -> throw IllegalArgumentException("Can't decode: $value into Double")
            }
          }
        },
        FileUpload::class.java to object : CustomTypeAdapter<FileUpload> {
          override fun decode(value: CustomTypeValue<*>): FileUpload {
            return FileUpload("", File(""));
          }

          override fun encode(value: FileUpload): CustomTypeValue<*> {
            return GraphQLString(value.mimetype)
          }
        },
        Map::class.java to object : DefaultCustomTypeAdapter<Map<*, *>>() {
          override fun decode(value: CustomTypeValue<*>): Map<*, *> {
            return if (value is GraphQLJsonObject) {
              value.value
            } else {
              throw IllegalArgumentException("Can't decode: $value into Map")
            }
          }
        },
        List::class.java to object : DefaultCustomTypeAdapter<List<*>>() {
          override fun decode(value: CustomTypeValue<*>): List<*> {
            return if (value is GraphQLJsonList) {
              value.value
            } else {
              throw IllegalArgumentException("Can't decode: $value into List")
            }
          }
        },
        Any::class.java to object : DefaultCustomTypeAdapter<Any>() {
          override fun decode(value: CustomTypeValue<*>): Any {
            return value.value!!
          }
        }
    )
  }
}
