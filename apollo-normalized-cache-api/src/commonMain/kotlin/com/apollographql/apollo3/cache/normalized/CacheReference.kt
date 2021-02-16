package com.apollographql.apollo3.cache.normalized

import kotlin.jvm.JvmStatic

class CacheReference(val key: String) {

  override fun equals(other: Any?): Boolean {
    return key == (other as? CacheReference)?.key
  }

  override fun hashCode(): Int = key.hashCode()

  override fun toString(): String = key

  fun serialize(): String {
    return "$SERIALIZATION_TEMPLATE{$key}"
  }

  companion object {
    private val SERIALIZATION_REGEX_PATTERN = Regex("ApolloCacheReference\\{(.*)\\}")
    private const val SERIALIZATION_TEMPLATE = "ApolloCacheReference"

    @JvmStatic
    fun deserialize(serializedCacheReference: String): CacheReference {
      val values = SERIALIZATION_REGEX_PATTERN.matchEntire(serializedCacheReference)?.groupValues
      require(values != null && values.size > 1) {
        "Not a cache reference: $serializedCacheReference Must be of the form: $SERIALIZATION_TEMPLATE{%s}"
      }
      return CacheReference(values[1])
    }

    @JvmStatic
    fun canDeserialize(value: String): Boolean {
      return SERIALIZATION_REGEX_PATTERN.matches(value)
    }
  }

}
