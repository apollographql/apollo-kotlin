@file:JvmMultifileClass
@file:JvmName("Assertions")

package com.apollographql.apollo3.api

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
