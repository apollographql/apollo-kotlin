package com.apollographql.apollo3.compiler.operationoutput

import com.apollographql.apollo3.compiler.applyIf
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.source
import java.io.File

/**
 * [OperationOutput] is a map where the operationId is the key and [OperationDescriptor] the value
 *
 * By default the operationId is a sha256 but it can be changed for custom whitelisting implementations
 */
typealias OperationOutput = Map<String, OperationDescriptor>

/**
 * This structure is also generated by other tools (iOS, cli, ...), try to keep the field names if possible.
 */
@JsonClass(generateAdapter = true)
class OperationDescriptor(
    val name: String,
    val source: String
)

private fun operationOutputAdapter(indent: String = ""): JsonAdapter<OperationOutput> {
  val moshi = Moshi.Builder().build()
  val type = Types.newParameterizedType(Map::class.java, String::class.java, OperationDescriptor::class.java)
  return moshi.adapter<OperationOutput>(type).indent(indent)
}

fun OperationOutput.toJson(indent: String = ""): String {
  return operationOutputAdapter(indent).toJson(this)
}

fun OperationOutput(file: File): OperationOutput {
  return try {
    file.source().buffer().use {
      operationOutputAdapter().fromJson(it)!!
    }
  } catch (e: Exception) {
    throw IllegalArgumentException("cannot parse operation output $file")
  }
}

fun OperationOutput.findOperationId(name: String): String {
  val id = entries.find { it.value.name == name }?.key
  check(id != null) {
    "cannot find operation ID for '$name', check your operationOutput.json"
  }
  return id
}
