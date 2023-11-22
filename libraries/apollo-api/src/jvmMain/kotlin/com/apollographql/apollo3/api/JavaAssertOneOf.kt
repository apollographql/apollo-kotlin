package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.Optional.Absent
import com.apollographql.apollo3.api.Optional.Present

object JavaAssertOneOf {
  @JvmStatic
  fun assertOneOf(vararg args: Optional<*>) {
    val presentArgs = args.filterIsInstance<Present<*>>()
    if (presentArgs.size != 1) {
      throw IllegalArgumentException("@oneOf input must have one field set (got ${presentArgs.size})")
    }
    // Possible cases:
    // - Optional<Type> (value can be null)
    // - Optional<Optional<Type>> (value can be Absent or Present but not Present(null))
    val presentArg = presentArgs.first()
    if (presentArg.value == null || presentArg.value == Absent) {
      throw IllegalArgumentException("The value set on @oneOf input field must be non-null")
    }
  }

  @JvmStatic
  @SafeVarargs
  @Suppress("NewApi")
  fun assertOneOf(vararg args: java.util.Optional<out java.util.Optional<*>>) {
    val presentArgs = args.filter { it.isPresent }
    if (presentArgs.size != 1) {
      throw IllegalArgumentException("@oneOf input must have one field set (got ${presentArgs.size})")
    }
    if (presentArgs.first().get().isEmpty) {
      throw IllegalArgumentException("The value set on @oneOf input field must be non-null")
    }
  }

  @JvmStatic
  @SafeVarargs
  fun assertOneOf(vararg args: com.google.common.base.Optional<out com.google.common.base.Optional<*>>) {
    val presentArgs = args.filter { it.isPresent }
    if (presentArgs.size != 1) {
      throw IllegalArgumentException("@oneOf input must have one field set (got ${presentArgs.size})")
    }
    if (!presentArgs.first().get().isPresent) {
      throw IllegalArgumentException("The value set on @oneOf input field must be non-null")
    }
  }
}
