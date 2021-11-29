package com.apollographql.apollo3.compiler

import okio.Buffer

/**
 * A variation of [String.capitalize] that:
 * - skips initial underscore, especially found in introspection queries
 * - is Locale independent so that it works the same way on all machines, including in the turkish locale
 * that uses a different 'I'
 */
fun String.capitalizeFirstLetter(): String {
  val builder = StringBuilder(length)
  var isCapitalized = false
  forEach {
    builder.append(if (!isCapitalized && it.isLetter()) {
      isCapitalized = true
      it.toString().uppercase()
    } else {
      it.toString()
    })
  }
  return builder.toString()
}

/**
 * A variation of [String.decapitalize] that:
 * - skips initial underscore, especially found in introspection queries
 * - is Locale independent so that it works the same way on all machines, including in the turkish locale
 * that uses a different 'I'
 */
fun String.decapitalizeFirstLetter(): String {
  val builder = StringBuilder(length)
  var isDecapitalized = false
  forEach {
    builder.append(if (!isDecapitalized && it.isLetter()) {
      isDecapitalized = true
      it.toString().lowercase()
    } else {
      it.toString()
    })
  }
  return builder.toString()
}

internal fun String.buffer() = Buffer().writeUtf8(this)