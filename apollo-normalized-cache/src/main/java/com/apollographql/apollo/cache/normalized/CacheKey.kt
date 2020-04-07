package com.apollographql.apollo.cache.normalized

/**
 * A key for a [Record] used for normalization in a [NormalizedCache].
 * If the json object which the [Record] corresponds to does not have a suitable
 * key, return use [NO_KEY].
 */
class CacheKey(val key: String) {

  @Deprecated(message = "Use property instead", replaceWith = ReplaceWith(expression = "key"))
  fun key(): String = key

  override fun equals(other: Any?): Boolean {
    return key == (other as? CacheKey)?.key
  }

  override fun hashCode(): Int = key.hashCode()

  override fun toString(): String = key

  companion object {

    @JvmField
    val NO_KEY = CacheKey("")

    @JvmStatic
    @Deprecated("Use constructor to instantiate CacheKey", replaceWith = ReplaceWith(expression = "CacheKey(key)"))
    fun from(key: String): CacheKey = CacheKey(key)
  }

}
