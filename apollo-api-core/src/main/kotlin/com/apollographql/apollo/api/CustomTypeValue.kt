package com.apollographql.apollo.api

/**
 * A wrapper class for representation of custom GraphQL type value, used in user provided [CustomTypeAdapter]
 * encoding / decoding functions.
 **/
abstract class CustomTypeValue<T> private constructor(val value: T) {

  /**
   * Represents a `String` value
   */
  class GraphQLString(value: String) : CustomTypeValue<String>(value)

  /**
   * Represents a `Boolean` value
   */
  class GraphQLBoolean(value: Boolean) : CustomTypeValue<Boolean>(value)

  /**
   * Represents a `Number` value
   */
  class GraphQLNumber(value: Number) : CustomTypeValue<Number>(value)

  /**
   * Represents a JSON `Object` value
   */
  class GraphQLJsonObject(value: Map<String, Any>) : CustomTypeValue<Map<String, Any>>(value)

  /**
   * Represents a JSON `List` value
   */
  class GraphQLJsonList(value: List<Any>) : CustomTypeValue<List<Any>>(value)

  companion object {

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun fromRawValue(value: Any): CustomTypeValue<*> {
      return when (value) {
        is Map<*, *> -> GraphQLJsonObject(value as Map<String, Any>)
        is List<*> -> GraphQLJsonList(value as List<Any>)
        is Boolean -> GraphQLBoolean(value)
        is Number -> GraphQLNumber(value)
        else -> GraphQLString(value.toString())
      }
    }
  }
}
