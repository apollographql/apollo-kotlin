package com.apollographql.apollo.compiler.operationoutput

import com.apollographql.apollo.compiler.applyIf
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.buffer
import okio.source
import java.io.File

@JsonClass(generateAdapter = true)
class OperationDescriptor(
    val name: String,
    val source: String
)

typealias OperationOutput = Map<String, OperationDescriptor>

fun adapter(indent: String? = null): JsonAdapter<OperationOutput> {
  val moshi = Moshi.Builder().build()
  val type = Types.newParameterizedType(Map::class.java, String::class.java, OperationDescriptor::class.java)
  return moshi.adapter<OperationOutput>(type).applyIf(indent != null) {
    this.indent(indent!!)
  }
}

fun OperationOutput(file: File): OperationOutput {
  return file.source().buffer().use {
    adapter().fromJson(it)!!
  }
}

fun OperationOutput.toJson(indent: String? = null): String {
  return adapter(indent).toJson(this)
}
