package com.apollographql.apollo.compiler

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

internal fun upperCamelCaseIgnoringNonLetters(strings: Collection<String>): String {
  return strings.map {
    it.capitalizeFirstLetter()
  }.joinToString("")
}

internal fun lowerCamelCaseIgnoringNonLetters(strings: Collection<String>): String {
  return strings.map {
    it.decapitalizeFirstLetter()
  }.joinToString("")
}

/**
 * On case-insensitive filesystems, we need to make sure two schema types with
 * different cases like 'Url' and 'URL' are not generated or their files will
 * overwrite each other.
 *
 * For Kotlin, we _could_ just change the file name (and not the class name) but
 * that only postpones the issue to later on when .class files are generated.
 *
 * In order to get predictable results independently of the system, we make the
 * case-insensitive checks no matter the actual filesystem.
 */
internal fun uniqueName(name: String, usedNames: Set<String>): String {
  var i = 1
  var uniqueName = name
  while (uniqueName.lowercase() in usedNames) {
    uniqueName = "${name}$i"
    i++
  }
  return uniqueName
}

internal fun String.withUnderscorePrefix(): String = if (this == "__typename") this else "_$this"

internal fun String.maybeAddSuffix(suffix: String): String {
  return if (this.endsWith(suffix)) {
    this
  } else {
    "$this$suffix"
  }
}


/**
 * Return the packageName if this file is in these roots or throw else
 */
internal fun String.toPackageName(): String {
  return split('/')
      .filter { it.isNotBlank() }
      .dropLast(1)
      .joinToString(".")
}

