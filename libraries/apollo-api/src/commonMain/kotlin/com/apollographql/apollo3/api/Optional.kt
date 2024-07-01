package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.Optional.Absent
import com.apollographql.apollo.api.Optional.Present
import com.apollographql.apollo.exception.MissingValueException
import kotlin.jvm.JvmStatic

/**
 * A sealed class that can either be [Present] or [Absent]
 *
 * It provides a convenient way to distinguish the case when a value is provided explicitly and should be
 * serialized (even if it's null) and the case where it's absent and shouldn't be serialized.
 */
sealed class Optional<out V> {
  /**
   * Returns the value if this [Optional] is [Present] or null else.
   */
  fun getOrNull(): V? = (this as? Present)?.value

  /**
   * Returns the value if this [Optional] is [Present] or throws [MissingValueException] else.
   */
  fun getOrThrow(): V {
    if (this is Present) {
      return value
    }
    throw MissingValueException()
  }

  data class Present<V>(val value: V) : Optional<V>()
  object Absent : Optional<Nothing>()

  companion object {
    @JvmStatic
    fun absent(): Absent = Absent

    @JvmStatic
    fun <V> present(value: V): Present<V> = Present(value)

    @JvmStatic
    fun <V : Any> presentIfNotNull(value: V?): Optional<V> = if (value == null) Absent else Present(value)
  }
}

/**
 * Returns the value if this [Optional] is [Present] or fallback else.
 */
fun <V> Optional<V>.getOrElse(fallback: V): V = if (this is Present) this.value else fallback

@ApolloExperimental
fun <V, R> Optional<V>.map(mapper: (V) -> R): Optional<R> {
  return when(this) {
    is Absent -> Absent
    is Present -> Optional.present(mapper(value))
  }
}
