package com.apollographql.apollo3.cache.normalized

import kotlin.jvm.JvmStatic

/**
 */
class CacheKey(val key: String) {

  override fun hashCode() = key.hashCode()
  override fun equals(other: Any?): Boolean {
    return key == (other as? CacheKey)?.key
  }
  override fun toString() = "CacheKey($key)"

  fun serialize(): String {
    return "$SERIALIZATION_TEMPLATE{$key}"
  }

  companion object {
    // IntelliJ complains about the invalid escape but looks like JS still needs it.
    // See https://youtrack.jetbrains.com/issue/KT-47189
    @Suppress("RegExpRedundantEscape")
    private val SERIALIZATION_REGEX_PATTERN = Regex("ApolloCacheReference\\{(.*)\\}")
    private const val SERIALIZATION_TEMPLATE = "ApolloCacheReference"

    @JvmStatic
    fun deserialize(serializedCacheKey: String): CacheKey {
      val values = SERIALIZATION_REGEX_PATTERN.matchEntire(serializedCacheKey)?.groupValues
      require(values != null && values.size > 1) {
        "Not a cache reference: $serializedCacheKey Must be of the form: $SERIALIZATION_TEMPLATE{%s}"
      }
      return CacheKey(values[1])
    }

    @JvmStatic
    fun canDeserialize(value: String): Boolean {
      return SERIALIZATION_REGEX_PATTERN.matches(value)
    }
  }
}
