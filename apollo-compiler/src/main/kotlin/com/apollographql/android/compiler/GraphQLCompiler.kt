package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.CodeGenerationIR
import com.apollographql.android.compiler.ir.TypeDeclaration
import com.squareup.javapoet.JavaFile
import com.squareup.moshi.Moshi
import java.io.File

open class GraphQLCompiler {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(CodeGenerationIR::class.java)

  fun write(irFile: File, outputDir: File, customTypeMap: Map<String, String> = emptyMap()) {
    val ir = irAdapter.fromJson(irFile.readText())
    val irPackageName = irFile.absolutePath.formatPackageName()
    val fragmentsPackage = if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
    val typesPackage = if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
    val supportedScalarTypeMapping = customTypeMap.supportedScalarTypeMapping(ir.typesUsed)
    val context = CodeGenerationContext(
        reservedTypeNames = emptyList(),
        typeDeclarations = ir.typesUsed,
        fragmentsPackage = fragmentsPackage,
        typesPackage = typesPackage,
        customTypeMap = supportedScalarTypeMapping
    )
    ir.writeTypeUsed(context, outputDir)
    ir.writeFragments(context, outputDir)
    ir.writeOperations(context, irPackageName, outputDir)
  }

  private fun CodeGenerationIR.writeFragments(context: CodeGenerationContext, outputDir: File) {
    fragments.forEach {
      val typeSpec = it.toTypeSpec(context)
      JavaFile.builder(context.fragmentsPackage, typeSpec).build().writeTo(outputDir)
    }
  }

  private fun CodeGenerationIR.writeTypeUsed(context: CodeGenerationContext, outputDir: File) {
    typesUsed.supportedTypeDeclarations().forEach {
      val typeSpec = it.toTypeSpec(context)
      JavaFile.builder(context.typesPackage, typeSpec).build().writeTo(outputDir)
    }

    if (context.customTypeMap.isNotEmpty()) {
      val typeSpec = CustomEnumTypeSpecBuilder(context).build()
      JavaFile.builder(context.typesPackage, typeSpec).build().writeTo(outputDir)
    }
  }

  private fun CodeGenerationIR.writeOperations(context: CodeGenerationContext, irPackageName: String, outputDir: File) {
    operations.map { OperationTypeSpecBuilder(it, fragments) }
        .forEach {
          val packageName = it.operation.filePath.formatPackageName()
          val typeSpec = it.toTypeSpec(context)
          JavaFile.builder(packageName, typeSpec).build().writeTo(outputDir)
        }
  }

  private fun String.formatPackageName(): String {
    val parts = split(File.separatorChar)
    (parts.size - 1 downTo 2)
        .filter { parts[it - 2] == "src" && parts[it] == "graphql" }
        .forEach { return parts.subList(it + 1, parts.size).dropLast(1).joinToString(".") }
    throw IllegalArgumentException("Files must be organized like src/main/graphql/...")
  }

  private fun List<TypeDeclaration>.supportedTypeDeclarations() =
      filter { it.kind == TypeDeclaration.KIND_ENUM || it.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }

  private fun Map<String, String>.supportedScalarTypeMapping(typeDeclarations: List<TypeDeclaration>) =
      typeDeclarations.filter { it.kind == TypeDeclaration.KIND_SCALAR_TYPE }
          .associate { it.name to (this[it.name] ?: "Object") }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
  }
}
