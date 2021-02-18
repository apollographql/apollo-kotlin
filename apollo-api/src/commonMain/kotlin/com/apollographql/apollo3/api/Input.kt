package com.apollographql.apollo3.api

/**
 * [Input] is a holder class that has a value only if isPresent is true
 *
 * It uses the same trick as [kotlin.Result] to store the value as Any so that Result(Absent) can be any type of Result<T>
 *
 * It provides a convenient way to distinguish the case when a value is provided explicitly and should be
 * serialized (even if it's null) and the case where it's absent and shouldn't be serialized.
 */
class Input<out V>(private val value: Any?) {
  val isPresent = value !is Absent

  fun getOrThrow(): V {
    if (value is Absent) {
      throw IllegalStateException("Input has no value")
    }

    return value as V
  }

  object Absent
  companion object {
    fun <V> present(value: V) = Input<V>(value)
    fun <V> absent() = Input<V>(Absent)
  }
}