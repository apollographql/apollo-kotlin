package com.apollographql.ijplugin.util

fun String.quoted(): String {
  return if (this.startsWith('"') && this.endsWith('"')) this else "\"$this\""
}

fun String.unquoted(): String {
  return removeSurrounding("\"")
}
