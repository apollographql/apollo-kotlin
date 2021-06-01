package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.squareup.moshi.Moshi
import okio.buffer
import okio.sink
import java.io.File

fun Ir.dumpTo(file: File) {
  val map = mutableMapOf<String, Any?>()

  operations.forEach {
    map.put(it.name, it.compiledField.toMap())
  }

  file.sink().buffer().use {
    Moshi.Builder().build().adapter(Any::class.java).toJson(it, map)
  }
}

private fun IrCompiledField.toMap(): Map<String, Any?> {
  return mapOf(
      "responseName" to responseName,
      "type" to type.toString(),
      "fieldSets" to fieldSets.map {
        it.name() to it.toMap()
      }.toMap()
  )
}

private val IrCompiledField.responseName
  get() = alias ?: name

private fun IrCompiledFieldSet.name() = typeSet.sorted().map { it.capitalizeFirstLetter() }.joinToString("")
private fun IrCompiledFieldSet.toMap(): Map<String, Any?> {
  return mapOf(
      "typeSet" to name(),
      "possibleTypes" to possibleTypes.sorted().joinToString(", "),
      "fields" to compiledFields.map {
        it.responseName to it.toMap()
      }.toMap()
  )
}