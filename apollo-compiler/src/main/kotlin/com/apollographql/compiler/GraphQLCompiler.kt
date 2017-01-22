package com.apollographql.compiler

import com.apollographql.compiler.ir.CodeGenerator
import com.apollographql.compiler.ir.Fragment
import com.apollographql.compiler.ir.OperationIntermediateRepresentation
import com.apollographql.compiler.ir.TypeDeclaration
import com.squareup.javapoet.JavaFile
import com.squareup.moshi.Moshi
import java.io.File

open class GraphQLCompiler {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(OperationIntermediateRepresentation::class.java)

  fun write(irFile: File, outputDir: File, generateClasses: Boolean = false) {
    val ir = irAdapter.fromJson(irFile.readText())
    val irPackageName = irFile.absolutePath.formatPackageName()
    val fragmentsPackage = "$irPackageName.fragment"
    val typesPackage = "$irPackageName.type"

    val operationTypeBuilders = ir.operations.map { OperationTypeSpecBuilder(it, ir.fragments) }
    (operationTypeBuilders + ir.fragments + ir.typesUsed).forEach {
      JavaFile.builder(javaFilePackageName(it, irPackageName, fragmentsPackage, typesPackage),
          it.toTypeSpec(!generateClasses, emptyList(), ir.typesUsed, fragmentsPackage, typesPackage))
          .build()
          .writeTo(outputDir)
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

  private fun javaFilePackageName(generator: CodeGenerator, irPackage: String, fragmentsPackage: String,
      typesPackage: String): String {
    when (generator) {
      is OperationTypeSpecBuilder -> return generator.operation.filePath.formatPackageName()
      is Fragment -> return fragmentsPackage
      is TypeDeclaration -> return typesPackage
    }
    return irPackage
  }
}
