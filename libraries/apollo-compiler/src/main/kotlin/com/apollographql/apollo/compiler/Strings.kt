package com.apollographql.apollo.compiler

/**
 * A variation of [String.capitalize] that:
 * - skips initial underscore, especially found in introspection queries
 * - is Locale independent so that it works the same way on all machines, including in the turkish locale
 * that uses a different 'I'
 */
fun String.capitalizeFirstLetter(): String {
  return replaceFirstLetter { it.toString().uppercase() }
}

/**
 * A variation of [String.decapitalize] that:
 * - skips initial underscore, especially found in introspection queries
 * - is Locale independent so that it works the same way on all machines, including in the turkish locale
 * that uses a different 'I'
 */
fun String.decapitalizeFirstLetter(): String {
  return replaceFirstLetter { it.toString().lowercase() }
}

private inline fun String.replaceFirstLetter(transform: (Char) -> String): String {
  val index = indexOfFirst { it.isLetter() }
  if (index == -1) return this
  val replacement = transform(this[index])
  if (replacement.length == 1 && replacement[0] == this[index]) return this
  return buildString(length) {
    append(this@replaceFirstLetter, 0, index)
    append(replacement)
    append(this@replaceFirstLetter, index + 1, this@replaceFirstLetter.length)
  }
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
