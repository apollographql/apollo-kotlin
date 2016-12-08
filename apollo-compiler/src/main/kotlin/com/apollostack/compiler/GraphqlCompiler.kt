package com.apollostack.compiler

import com.apollostack.compiler.ir.QueryIntermediateRepresentation
import com.squareup.javapoet.JavaFile
import com.squareup.moshi.Moshi
import java.io.File

open class GraphqlCompiler {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(QueryIntermediateRepresentation::class.java)

  fun write(irFile: File) {
    val packageName = irFile.absolutePath.formatPackageName()
    val ir = irAdapter.fromJson(irFile.readText())
    val outputDir = OUTPUT_DIRECTORY.fold(File("build"), ::File)
    val operationTypeSpecBuilders = ir.operations.map {
      OperationTypeSpecBuilder(it.operationName, it.fields, ir.fragments)
    }
    (operationTypeSpecBuilders + ir.typesUsed + ir.fragments)
        .map { JavaFile.builder(packageName, it.toTypeSpec()).build() }
        .forEach { it.writeTo(outputDir) }
  }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
  }

  private fun String.formatPackageName(): String {
    val parts = split(File.separatorChar)

    for (i in 2..parts.size) {
      if (parts[i - 2] == "src" && parts[i] == "graphql") {
        return parts.subList(i + 1, parts.size).dropLast(1).joinToString(".")
      }
    }
    throw IllegalArgumentException("Files must be organized like src/main/graphql/...")
  }
}
