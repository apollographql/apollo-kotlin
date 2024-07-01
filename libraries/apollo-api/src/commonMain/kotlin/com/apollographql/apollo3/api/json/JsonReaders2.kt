/**
 * The reason this file is named JsonReaders2 is that JsonReaders was made inaccessible from Java, and we couldn't change that
 * without breaking binary compatibility
 */
@file:JvmName("JsonReaders")
package com.apollographql.apollo.api.json

import kotlin.jvm.JvmName

fun JsonReader.readTypename(): String {
  val names = listOf("__typename")
  val index = selectName(names)
  check(index == 0) {
    error("__typename not found")
  }
  return nextString() ?: error("__typename is null")
}