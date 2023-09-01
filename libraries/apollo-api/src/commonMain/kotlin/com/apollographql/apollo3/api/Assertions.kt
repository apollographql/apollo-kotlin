@file:JvmName("Assertions")
package com.apollographql.apollo3.api

import com.apollographql.apollo3.exception.DefaultApolloException
import kotlin.jvm.JvmName

/**
 * Helper function for the JavaCodegen
 */
fun checkFieldNotMissing(value: Any?, name: String) {
  if (value == null) {
    throw DefaultApolloException("Field '$name' is missing")
  }
}
