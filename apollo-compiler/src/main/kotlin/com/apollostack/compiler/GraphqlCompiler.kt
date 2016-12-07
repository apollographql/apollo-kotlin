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
    ir.operations.forEach {
      val typeSpecBuilder = OperationTypeSpecBuilder(it.operationName, it.fields)
      JavaFile
          .builder(packageName, typeSpecBuilder.build())
          .build()
          .writeTo(OUTPUT_DIRECTORY.fold(File("build"), ::File))
    }
    ir.typesUsed.forEach {
      JavaFile
          .builder(packageName, it.toTypeSpec())
          .build()
          .writeTo(OUTPUT_DIRECTORY.fold(File("build"), ::File))
    }
  }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
  }

  private fun String.formatPackageName(): String {
    val parts = split(File.separatorChar)
    val srcFolderIndex = parts.indexOfFirst { it == "src" }
    val graphqlFolderIndex = parts.indexOfFirst { it == "graphql" }
    if (graphqlFolderIndex - srcFolderIndex != 2) {
      throw IllegalArgumentException("Files must be organized like src/main/graphql/...")
    }

    return parts.subList(graphqlFolderIndex + 1, parts.size).dropLast(1).joinToString(".")
  }
}