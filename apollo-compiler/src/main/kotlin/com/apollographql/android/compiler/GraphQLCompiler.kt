package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.*
import com.squareup.javapoet.JavaFile
import com.squareup.moshi.Moshi
import java.io.File

open class GraphQLCompiler {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(OperationIntermediateRepresentation::class.java)

  fun write(irFile: File, outputDir: File, generateClasses: Boolean = false,
      customTypeMap: Map<String, String> = emptyMap()) {
    val ir = irAdapter.fromJson(irFile.readText())
    val irPackageName = irFile.absolutePath.formatPackageName()
    val fragmentsPackage = if (irPackageName.length > 0) "$irPackageName.fragment" else "fragment"
    val typesPackage = if (irPackageName.length > 0) "$irPackageName.type" else "type"
    val codeGenerationContext = CodeGenerationContext(!generateClasses, emptyList(), ir.typesUsed, fragmentsPackage,
        typesPackage, customTypeMap)
    val operationTypeBuilders = ir.operations.map { OperationTypeSpecBuilder(it, ir.fragments) }
    (operationTypeBuilders + ir.fragments + ir.typesUsed).forEach {
      val packageName = javaFilePackageName(it, irPackageName, fragmentsPackage, typesPackage)
      val typeSpec = it.toTypeSpec(codeGenerationContext)
      JavaFile.builder(packageName, typeSpec).build().writeTo(outputDir)
    }

    if (customTypeMap.isNotEmpty()) {
      val typeSpec = CustomEnumTypeSpecBuilder(codeGenerationContext).build()
      JavaFile.builder(typesPackage, typeSpec).build().writeTo(outputDir)
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
