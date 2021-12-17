package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

@Deprecated("Input is a helper class to help migrating to 3.x " +
    "and will be removed in a future version")
@ApolloDeprecatedSince(v3_0_0)
class Input {
  companion object {
    @JvmStatic
    @Deprecated("optional() is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("Optional.presentIfNotNull(value)"))
    @ApolloDeprecatedSince(v3_0_0)
    fun <V> optional(value: V): Optional<V> {
      return Optional.presentIfNotNull(value)
    }

    @JvmStatic
    @Deprecated("fromNullable() is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("Optional.Present(value)"))
    @ApolloDeprecatedSince(v3_0_0)
    fun <V> fromNullable(value: V): Optional<V> {
      return Optional.Present(value)
    }

    @JvmStatic
    @Deprecated("absent() is a helper function to help migrating to 3.x " +
        "and will be removed in a future version", ReplaceWith("Optional.Absent"))
    @ApolloDeprecatedSince(v3_0_0)
    fun <V> absent(): Optional.Absent {
      return Optional.Absent
    }
  }
}

@Deprecated("toInput() is a helper function to help migrating to 3.x " +
    "and will be removed in a future version", ReplaceWith("Optional.presentIfNotNull(this)"))
@ApolloDeprecatedSince(v3_0_0)
@JvmName("-toInputOrAbsent")
fun <T> T.toInput(): Optional<T> = Optional.presentIfNotNull(this)

@Deprecated("toInput() is a helper function to help migrating to 3.x " +
    "and will be removed in a future version", ReplaceWith("Optional.Present(this)"))
@ApolloDeprecatedSince(v3_0_0)
@JvmName("-toInput")
fun <T : Any> T.toInput(): Optional<T> = Optional.Present(this)
