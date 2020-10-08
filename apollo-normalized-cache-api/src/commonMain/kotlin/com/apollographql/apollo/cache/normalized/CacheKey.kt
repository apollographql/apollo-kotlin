package com.apollographql.apollo.cache.normalized

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A key for a [Record] used for normalization in a [NormalizedCache].
 * If the json object which the [Record] corresponds to does not have a suitable
 * key, return use [NO_KEY].
 */
class CacheKey(val key: String) {

  override fun equals(other: Any?): Boolean {
    return key == (other as? CacheKey)?.key
  }

  override fun hashCode(): Int = key.hashCode()

  override fun toString(): String = key

  companion object {

    @JvmField
    val NO_KEY = CacheKey("")

    @JvmStatic
    fun from(key: String): CacheKey = CacheKey(key)
  }

}
