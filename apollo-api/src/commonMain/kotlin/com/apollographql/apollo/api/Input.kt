package com.apollographql.apollo.api

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A wrapper class to provide a [value] with a [defined] state.
 *
 * Wrapper can be in one of 2 states:
 * - *defined* (`defined == true`) means reference to [value] is set explicitly
 * - *undefined* (`defined == false`) means reference is not set.
 *
 * It provides a convenience way to distinguish the case when [value] is provided explicitly and should be
 * serialized (even if it's null) and the case when [value] is undefined (means it won't be serialized).
 */
class Input<V> internal constructor(
    @JvmField val value: V?,
    @JvmField val defined: Boolean
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Input<*>) return false

    if (value != other.value) return false
    if (defined != other.defined) return false

    return true
  }

  override fun hashCode(): Int {
    var result = value?.hashCode() ?: 0
    result = 31 * result + defined.hashCode()
    return result
  }

  override fun toString(): String {
    return "Input(value = $value, defined = $defined)"
  }

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
