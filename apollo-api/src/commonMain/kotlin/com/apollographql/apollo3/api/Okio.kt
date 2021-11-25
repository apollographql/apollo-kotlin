@file:JvmName("Okio")
package com.apollographql.apollo3.api

import okio.Buffer
import okio.ByteString
import okio.Source
import kotlin.jvm.JvmName

fun String.source(): Source {
  return Buffer().writeUtf8(this)
}

fun ByteString.source(): Source {
  return Buffer().write(this)
}

fun ByteArray.source(): Source {
  return Buffer().write(this)
}
