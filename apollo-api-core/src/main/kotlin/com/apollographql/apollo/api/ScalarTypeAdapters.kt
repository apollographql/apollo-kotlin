package com.apollographql.apollo.api

class ScalarTypeAdapters(customAdapters: Map<ScalarType, CustomTypeAdapter<*>>) {
  private val customAdapters: MutableMap<String, CustomTypeAdapter<*>>

  init {
    this.customAdapters = HashMap(customAdapters.mapKeys { it.key.typeName() })
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> adapterFor(scalarType: ScalarType): CustomTypeAdapter<T> {
    val customTypeAdapter: CustomTypeAdapter<*>? = (customAdapters[scalarType.typeName()] ?: DEFAULT_ADAPTERS[scalarType.javaType()])
    return requireNotNull(customTypeAdapter) {
      "Can't map GraphQL type: ${scalarType.typeName()} to: %${scalarType.javaType()}. Did you forget to add a custom type adapter?"
    } as CustomTypeAdapter<T>
  }

  private abstract class DefaultCustomTypeAdapter<T : Any> : CustomTypeAdapter<T> {

    override fun encode(value: T): CustomTypeValue<*> {
      return CustomTypeValue.fromRawValue(value)
    }
  }

  companion object {
    private val DEFAULT_ADAPTERS = LinkedHashMap<Class<*>, CustomTypeAdapter<*>>().apply {

      this[String::class.java] = object : DefaultCustomTypeAdapter<String>() {
        override fun decode(value: CustomTypeValue<*>): String {
          return value.value.toString()
        }
      }

      this[Boolean::class.java] = object : DefaultCustomTypeAdapter<Boolean>() {
        override fun decode(value: CustomTypeValue<*>): Boolean {
          return when (value) {
            is CustomTypeValue.GraphQLBoolean -> value.value
            is CustomTypeValue.GraphQLString -> value.value.toBoolean()
            else -> throw IllegalArgumentException("Can't decode: $value into Boolean")
          }
        }
      }

      this[Int::class.java] = object : DefaultCustomTypeAdapter<Int>() {
        override fun decode(value: CustomTypeValue<*>): Int {
          return when (value) {
            is CustomTypeValue.GraphQLNumber -> value.value.toInt()
            is CustomTypeValue.GraphQLString -> value.value.toInt()
            else -> throw IllegalArgumentException("Can't decode: $value into Integer")
          }
        }
      }

      this[Long::class.java] = object : DefaultCustomTypeAdapter<Long>() {
        override fun decode(value: CustomTypeValue<*>): Long {
          return when (value) {
            is CustomTypeValue.GraphQLNumber -> value.value.toLong()
            is CustomTypeValue.GraphQLString -> value.value.toLong()
            else -> throw IllegalArgumentException("Can't decode: $value into Long")
          }
        }
      }

      this[Float::class.java] = object : DefaultCustomTypeAdapter<Float>() {
        override fun decode(value: CustomTypeValue<*>): Float {
          return when (value) {
            is CustomTypeValue.GraphQLNumber -> value.value.toFloat()
            is CustomTypeValue.GraphQLString -> value.value.toFloat()
            else -> throw IllegalArgumentException("Can't decode: $value into Float")
          }
        }
      }

      this[Double::class.java] = object : DefaultCustomTypeAdapter<Double>() {
        override fun decode(value: CustomTypeValue<*>): Double {
          return when (value) {
            is CustomTypeValue.GraphQLNumber -> value.value.toDouble()
            is CustomTypeValue.GraphQLString -> value.value.toDouble()
            else -> throw IllegalArgumentException("Can't decode: $value into Double")
          }
        }
      }

      this[FileUpload::class.java] = object : CustomTypeAdapter<FileUpload> {
        override fun decode(value: CustomTypeValue<*>): FileUpload {
          throw UnsupportedOperationException("Can't decode file custom scalar type")
        }

        override fun encode(value: FileUpload): CustomTypeValue<*> {
          return CustomTypeValue.GraphQLString("")
        }
      }

      this[Any::class.java] = object : DefaultCustomTypeAdapter<Any>() {
        override fun decode(value: CustomTypeValue<*>): Any {
          return value.value!!
        }
      }

      this[java.lang.Object::class.java] = object : DefaultCustomTypeAdapter<Any>() {
        override fun decode(value: CustomTypeValue<*>): Any {
          return value.value!!
        }
      }

      this[Map::class.java] = object : DefaultCustomTypeAdapter<Map<*, *>>() {
        override fun decode(value: CustomTypeValue<*>): Map<*, *> {
          return if (value is CustomTypeValue.GraphQLJsonObject) {
            value.value
          } else {
            throw IllegalArgumentException("Can't decode: $value into Map")
          }
        }
      }

      this[List::class.java] = object : DefaultCustomTypeAdapter<List<*>>() {
        override fun decode(value: CustomTypeValue<*>): List<*> {
          return if (value is CustomTypeValue.GraphQLJsonList) {
            value.value
          } else {
            throw IllegalArgumentException("Can't decode: $value into List")
          }
        }
      }
    }
  }
}
