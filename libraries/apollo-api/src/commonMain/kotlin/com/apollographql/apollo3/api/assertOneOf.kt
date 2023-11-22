package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.Optional.Present

fun assertOneOf(vararg args: Optional<*>) {
  val presentArgs = args.filterIsInstance<Present<*>>()
  if (presentArgs.size != 1) {
    throw IllegalArgumentException("@oneOf input must have one field set (got ${presentArgs.size})")
  }
  if (presentArgs.first().value == null) {
    throw IllegalArgumentException("The value set on @oneOf input field must be non-null")
  }
}