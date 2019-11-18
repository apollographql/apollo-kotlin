package com.apollographql.apollo.api

/**
 * An immutable object that wraps reference to another object.
 * Reference can be in two states:
 * - *defined* means reference to another object is set explicitly
 * - *undefined* means reference is not set.
 *
 * It provides a convenience way to distinguish the case when value is provided explicitly and should be
 * serialized (even if it's null) and the case when value is undefined (means it won't be serialized).
 *
 * @param <V> the type of instance that can be contained
 */
class Input<V> private constructor(
    @JvmField val value: V?,
    @JvmField val defined: Boolean
) {

  companion object {

    /**
     * Creates a new [Input] instance that is defined in case if `value` is not-null
     * and undefined otherwise.
     *
     * @param value to be wrapped
     * @return a new [Input] instance
     */
    @JvmStatic
    fun <V> optional(value: V?): Input<V> {
      return value?.let { fromNullable(it) } ?: absent()
    }

    /**
     * Creates a new [Input] instance that is always defined.
     *
     * @param value to be wrapped
     * @return a new [Input] instance
     */
    @JvmStatic
    fun <V> fromNullable(value: V?): Input<V> {
      return Input(value, true)
    }

    /**
     * Creates a new [Input] instance that is always undefined.
     *
     * @return a new [Input] instance
     */
    @JvmStatic
    fun <V> absent(): Input<V> {
      return Input(null, false)
    }
  }
}
