package com.apollographql.apollo3.api

import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

@Deprecated("Input is a helper class to help migrating to 3.x " +
    "and will be removed in a future version")
class Input {
  companion object {
    @JvmStatic
    @Deprecated("optional() is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("Optional.presentIfNotNull(value)"))
    fun <V> optional(value: V): Optional<V> {
      return Optional.presentIfNotNull(value)
    }

    @JvmStatic
    @Deprecated("fromNullable() is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("Optional.Present(value)"))
    fun <V> fromNullable(value: V): Optional<V> {
      return Optional.Present(value)
    }

    @JvmStatic
    @Deprecated("absent() is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("Optional.Absent"))
    fun <V> absent(): Optional.Absent {
      return Optional.Absent
    }
  }
}

@Deprecated("toInput() is a helper function to help migrating to 3.x " +
    "and will be removed in a future version", ReplaceWith("Optional.presentIfNotNull(this)"))
@JvmName("-toInputOrAbsent")
fun <T> T.toInput(): Optional<T> = Optional.presentIfNotNull(this)

@Deprecated("toInput() is a helper function to help migrating to 3.x " +
    "and will be removed in a future version", ReplaceWith("Optional.Present(this)"))
@JvmName("-toInput")
fun <T : Any> T.toInput(): Optional<T> = Optional.Present(this)