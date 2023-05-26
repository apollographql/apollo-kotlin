package com.apollographql.ijplugin.util

import java.util.Locale

fun String.quoted(): String {
  return if (this.startsWith('"') && this.endsWith('"')) this else "\"$this\""
}

fun String.unquoted(): String {
  return removeSurrounding("\"")
}

fun String.capitalizeFirstLetter() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
