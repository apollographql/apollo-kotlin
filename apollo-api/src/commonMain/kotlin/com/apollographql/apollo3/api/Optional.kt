package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.MissingValueException

/**
 * A sealed class that can either be [Present] or [Absent]
 *
 * It provides a convenient way to distinguish the case when a value is provided explicitly and should be
 * serialized (even if it's null) and the case where it's absent and shouldn't be serialized.
 */
sealed class Optional<out V> {
  fun getOrNull() = (this as Present).value
  fun getOrThrow() = getOrNull() ?: throw MissingValueException()

  class Present<V>(val value: V) : Optional<V>()
  object Absent : Optional<Nothing>()
}

