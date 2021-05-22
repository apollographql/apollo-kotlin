package com.apollographql.apollo3.compiler

import java.util.Locale


/**
 * A variation of [String.capitalize] that:
 * - skips initial underscore, especially found in introspection queries
 * - works with Kotlin 1.3 to be compatible with Gradle 6
 * - is Locale independent so that the compiler can
 */
fun String.capitalizeFirstLetter(): String {
  val builder = StringBuilder(length)
  var isCapitalized = false
  forEach {
    builder.append(if (!isCapitalized && it.isLetter()) {
      isCapitalized = true
      it.toString().toUpperCase(Locale.US)
    } else {
      it.toString()
    })
  }
  return builder.toString()
}

/**
 * A variation of [String.decapitalize] that:
 * - skips initial underscore, especially found in introspection queries
 * - works with Kotlin 1.3 to be compatible with Gradle 6
 * - is Locale independent so that the compiler can
 */
fun String.decapitalizeFirstLetter(): String {
  val builder = StringBuilder(length)
  var isDecapitalized = false
  forEach {
    builder.append(if (!isDecapitalized && it.isLetter()) {
      isDecapitalized = true
      it.toString().toLowerCase(Locale.US)
    } else {
      it.toString()
    })
  }
  return builder.toString()
}
