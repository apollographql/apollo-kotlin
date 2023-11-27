@file:JvmMultifileClass
@file:JvmName("Assertions")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.DefaultApolloException
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Helper function for the JavaCodegen
 */
fun checkFieldNotMissing(value: Any?, name: String) {
  if (value == null) {
    throw DefaultApolloException("Field '$name' is missing")
  }
}

fun assertOneOf(vararg args: Optional<*>) {
  val presentArgs = args.filterIsInstance<Optional.Present<*>>()
  if (presentArgs.size != 1) {
    throw IllegalArgumentException("@oneOf input must have one field set (got ${presentArgs.size})")
  }
  // Possible cases:
  // - Optional<Type> (value can be null)
  // - Optional<Optional<Type>> (value can be Absent or Present but not Present(null))
  val presentArg = presentArgs.first()
  if (presentArg.value == null || presentArg.value == Optional.Absent) {
    throw IllegalArgumentException("The value set on @oneOf input field must be non-null")
  }
}
