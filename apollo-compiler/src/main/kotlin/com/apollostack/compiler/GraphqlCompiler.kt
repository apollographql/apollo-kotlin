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
    (ir.typesUsed + ir.fragments).forEach {
      JavaFile.builder(packageName, it.toTypeSpec()).build().writeTo(outputDir)
    }

    ir.operations.forEach {
      val queryTypeSpec = QueryTypeSpecBuilder(it, ir.fragments).build()
      JavaFile.builder(packageName, queryTypeSpec).build().writeTo(outputDir)
    }
  }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
  }

  private fun String.formatPackageName(): String {
    val parts = split(File.separatorChar)
    (parts.size - 1 downTo 2)
        .filter { parts[it - 2] == "src" && parts[it] == "graphql" }
        .forEach { return parts.subList(it + 1, parts.size).dropLast(1).joinToString(".") }
    throw IllegalArgumentException("Files must be organized like src/main/graphql/...")
  }
}
