@file:JvmMultifileClass
@file:JvmName("Assertions")

package com.apollographql.apollo.api

import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.exception.NullOrMissingField
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Helper function for the Java codegen
 */
fun checkFieldNotMissing(value: Any?, name: String) {
  if (value == null) {
    throw NullOrMissingField("Field '$name' is missing or null")
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

/**
 * Helper function for the Kotlin codegen
 */
fun missingField(jsonReader: JsonReader, name: String): Nothing {
  throw NullOrMissingField("Field '$name' is missing or null at path ${jsonReader.getPath()}")
}
