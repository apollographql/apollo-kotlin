package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import kotlin.jvm.JvmStatic

/**
 * A [CacheKey] identifies an object in the cache.
 *
 * @param key The key of the object in the cache. The key must be globally unique.
 */
class CacheKey constructor(val key: String) {

  /**
   * Builds a [CacheKey] from a typename and a list of Strings.
   *
   * This can be used for the common case where [CacheKey] use [typename] as a namespace and [values] as a path.
   */
  constructor(typename: String, values: List<String>): this(
      buildString {
        append(typename)
        append(":")
        values.forEach {
          append(it)
        }
      }
  )

  /**
   * Builds a [CacheKey] from a typename and a list of Strings.
   *
   * This can be used for the common case where [CacheKey] use [typename] as a namespace and [values] as a path.
   */
  constructor(typename: String, vararg values: String) : this(typename, values.toList())

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

    private val ROOT_CACHE_KEY = CacheKey("QUERY_ROOT")

    @JvmStatic
    fun rootKey(): CacheKey {
      return ROOT_CACHE_KEY
    }

    /**
     * Helper function to build a cache key from a list of strings
     */
    @Deprecated("Use the constructor instead", ReplaceWith("CacheKey(typename, values)"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_0_0)
    fun from(typename: String, values: List<String>) = CacheKey(typename, values)

    /**
     * Helper function to build a cache key from a list of strings
     */
    @Deprecated("Use the constructor instead", ReplaceWith("CacheKey(typename, values)"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_0_0)
    fun from(typename: String, vararg values: String) = CacheKey(typename, values.toList())
  }
}
