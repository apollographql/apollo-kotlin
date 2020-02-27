package com.apollographql.apollo.api

/**
 * A wrapper class of provide [value] with a [defined] state.
 *
 * Wrapper can be in one of 2 states:
 * - *defined* (`defined == true`) means reference to [value] is set explicitly
 * - *undefined* (`defined == false`) means reference is not set.
 *
 * It provides a convenience way to distinguish the case when [value] is provided explicitly and should be
 * serialized (even if it's null) and the case when [value] is undefined (means it won't be serialized).
 */
class Input<V> private constructor(
    @JvmField val value: V?,
    @JvmField val defined: Boolean
) {

  companion object {

    /**
     * Creates a new [Input] instance that is defined in case if [value] is not-null
     * and undefined otherwise.
     */
    @JvmStatic
    fun <V> optional(value: V?): Input<V> {
      return value?.let { fromNullable(it) } ?: absent()
    }

    /**
     * Creates a new [Input] instance that is always defined for any provided [value].
     */
    @JvmStatic
    fun <V> fromNullable(value: V?): Input<V> {
      return Input(value, true)
    }

    /**
     * Creates a new [Input] instance that is always undefined.
     */
    @JvmStatic
    fun <V> absent(): Input<V> {
      return Input(null, false)
    }
  }
}
