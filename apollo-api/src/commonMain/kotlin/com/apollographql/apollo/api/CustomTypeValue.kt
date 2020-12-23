package com.apollographql.apollo.api

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A wrapper class for representation of custom GraphQL type value, used in user provided [CustomScalarTypeAdapter]
 * encoding / decoding functions.
 **/
sealed class CustomTypeValue<T>(@JvmField val value: T) {

  object GraphQLNull: CustomTypeValue<Unit>(Unit)

  /**
   * Represents a `String` value
   */
  class GraphQLString(value: String) : CustomTypeValue<String>(value) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is GraphQLString) return false
      if (value != other.value) return false
      return true
    }

    override fun hashCode(): Int {
      return value.hashCode()
    }
  }

  /**
   * Represents a `Boolean` value
   */
  class GraphQLBoolean(value: Boolean) : CustomTypeValue<Boolean>(value) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is GraphQLBoolean) return false
      if (value != other.value) return false
      return true
    }

    override fun hashCode(): Int {
      return value.hashCode()
    }
  }

  /**
   * Represents a `Number` value
   */
  class GraphQLNumber(value: Number) : CustomTypeValue<Number>(value) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is GraphQLNumber) return false
      if (value != other.value) return false
      return true
    }

    override fun hashCode(): Int {
      return value.hashCode()
    }
  }

  /**
   * Represents a JSON `Object` value
   */
  class GraphQLJsonObject(value: Map<String, Any>) : CustomTypeValue<Map<String, Any>>(value) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is GraphQLJsonObject) return false
      if (value != other.value) return false
      return true
    }

    override fun hashCode(): Int {
      return value.hashCode()
    }
  }

  /**
   * Represents a JSON `List` value
   */
  class GraphQLJsonList(value: List<Any>) : CustomTypeValue<List<Any>>(value) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is GraphQLJsonList) return false
      if (value != other.value) return false
      return true
    }

    override fun hashCode(): Int {
      return value.hashCode()
    }
  }

  companion object {

    // TODO: should this method accept nullable values in case a scalar type is represented as 'null'?
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun fromRawValue(value: Any): CustomTypeValue<*> {
      return when (value) {
        is Map<*, *> -> GraphQLJsonObject(value as Map<String, Any>)
        is List<*> -> GraphQLJsonList(value as List<Any>)
        is Boolean -> GraphQLBoolean(value)
        is BigDecimal -> GraphQLNumber(value.toNumber())
        is Number -> GraphQLNumber(value)
        else -> GraphQLString(value.toString())
      }
    }
  }
}
