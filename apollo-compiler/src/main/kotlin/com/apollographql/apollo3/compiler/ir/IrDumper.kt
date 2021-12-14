package com.apollographql.apollo3.compiler.ir

import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import java.io.File

fun Ir.dumpTo(file: File) {
  val map = mutableMapOf<String, Any?>()

  file.sink().buffer().use {
    Moshi.Builder().build().adapter(Any::class.java).toJson(it, map)
  }
}
