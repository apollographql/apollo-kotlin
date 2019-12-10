package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.internal.QueryDocumentMinifier
import java.io.File
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

internal class OperationOutput(val packageNameProvider: PackageNameProvider) {
  private var operations: List<Operation> = emptyList()

  fun visit(ir: CodeGenerationIR) {
    operations = operations + ir.operations
  }

  fun writeTo(outputDir: File) {
    if (operations.isEmpty()) return

    val jsonMap = operations.fold(mapOf<String, Map<String, String>>()) { acc, it ->
      val operationId = QueryDocumentMinifier.minify(it.sourceWithFragments).sha256()
      val operationInfo = mapOf("name" to it.operationName, "source" to QueryDocumentMinifier.minify(it.sourceWithFragments))
      acc.plus(
          operationId to operationInfo
      )
    }

    val nested = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    val type = Types.newParameterizedType(Map::class.java, String::class.java, nested)
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter<Map<String, Map<String, String>>>(type).indent("    ")
    val json = adapter.toJson(jsonMap)

    val filename = operations.first().filePath.substringAfterLast(File.separator)
    val name = filename.split('.').first()
    val outputFile = outputDir.resolve("$name.json").also {
      it.parentFile.mkdirs()
    }
    outputFile.writeText(json)
  }
}
