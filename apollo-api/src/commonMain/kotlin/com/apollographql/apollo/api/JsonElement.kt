package com.apollographql.apollo.api

/**
 * A wrapper class for representation of custom GraphQL type value, used in user provided [CustomScalarTypeAdapter]
 * encoding / decoding functions.
 **/
sealed class JsonElement {
  fun toRawValue(): Any? = when (this) {
    is JsonNull -> null
    is JsonString -> value
    is JsonBoolean -> value
    is JsonNumber -> value
    is JsonObject -> value.mapValues { it.value.toRawValue() }
    is JsonList -> value.map { it.toRawValue() }
  }

  object JsonNull: JsonElement()

  /**
   * Represents a `String` value
   */
  class JsonString(val value: String) : JsonElement()

  /**
   * Represents a `Boolean` value
   */
  class JsonBoolean(val value: Boolean) : JsonElement()

  /**
   * Represents a `Number` value
   */
  class JsonNumber(val value: Number) : JsonElement()

  /**
   * Represents a JSON `Object` value
   */
  class JsonObject(val value: Map<String, JsonElement>) : JsonElement()

  /**
   * Represents a JSON `List` value
   */
  class JsonList(val value: List<JsonElement>) : JsonElement()

  companion object {

    @Suppress("UNCHECKED_CAST")
    fun fromRawValue(value: Any?): JsonElement {
      return when (value) {
        is Map<*, *> -> JsonObject((value as Map<String, *>).mapValues { fromRawValue(it.value) })
        is List<*> -> JsonList(value.map { fromRawValue(it) })
        is Boolean -> JsonBoolean(value)
        is BigDecimal -> JsonNumber(value.toNumber())
        is Number -> JsonNumber(value)
        is String -> JsonString(value.toString())
        null -> JsonNull
        // What can happen here?
        else -> JsonString(value.toString())
      }
    }
  }
}
