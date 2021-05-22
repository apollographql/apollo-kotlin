package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import java.io.File

fun IntermediateRepresentation.dumpTo(file: File) {
  val map = mutableMapOf<String, Any?>()

  operations.forEach {
    map.put(it.name, it.field.toMap())
  }

  file.sink().buffer().use {
    Moshi.Builder().build().adapter(Any::class.java).toJson(it, map)
  }
}

private fun IrField.toMap(): Map<String, Any?> {
  return mapOf(
      "responseName" to info.responseName,
      "type" to info.type.toString(),
      "fieldSets" to fieldSets.map {
        it.name() to it.toMap()
      }.toMap()
  )
}

private fun IrFieldSet.name() = typeSet.sorted().map { it.capitalizeFirstLetter() }.joinToString("")

private fun IrFieldSet.toMap(): Map<String, Any?> {
  return mapOf(
      "typeSet" to name(),
      "possibleTypes" to possibleTypes.sorted().joinToString(", "),
      "fields" to fields.map {
        it.info.responseName to it.toMap()
      }.toMap()
  )
}