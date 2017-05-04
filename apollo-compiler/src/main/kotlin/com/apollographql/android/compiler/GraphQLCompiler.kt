package com.apollographql.android.compiler

import com.apollographql.android.compiler.ir.CodeGenerationContext
import com.apollographql.android.compiler.ir.CodeGenerationIR
import com.apollographql.android.compiler.ir.TypeDeclaration
import com.squareup.javapoet.JavaFile
import com.squareup.moshi.Moshi
import java.io.File

class GraphQLCompiler {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(CodeGenerationIR::class.java)

  fun write(args: Arguments) {
    val ir = irAdapter.fromJson(args.irFile.readText())
    val irPackageName = args.irFile.absolutePath.formatPackageName()
    val fragmentsPackage = if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
    val typesPackage = if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
    val supportedScalarTypeMapping = args.customTypeMap.supportedScalarTypeMapping(ir.typesUsed)
    val context = CodeGenerationContext(
        reservedTypeNames = emptyList(),
        typeDeclarations = ir.typesUsed,
        fragmentsPackage = fragmentsPackage,
        typesPackage = typesPackage,
        customTypeMap = supportedScalarTypeMapping,
        nullableValueType = args.nullableValueType,
        generateAccessors = args.generateAccessors,
        ir = ir)
    ir.writeJavaFiles(context, args.outputDir)
  }

  private fun CodeGenerationIR.writeJavaFiles(context: CodeGenerationContext, outputDir: File) {
    fragments.forEach {
      val typeSpec = it.toTypeSpec(context.copy())
      JavaFile.builder(context.fragmentsPackage, typeSpec).build().writeTo(outputDir)
    }

    typesUsed.supportedTypeDeclarations().forEach {
      val typeSpec = it.toTypeSpec(context.copy())
      JavaFile.builder(context.typesPackage, typeSpec).build().writeTo(outputDir)
    }

    if (context.customTypeMap.isNotEmpty()) {
      val typeSpec = CustomEnumTypeSpecBuilder(context.copy()).build()
      JavaFile.builder(context.typesPackage, typeSpec).build().writeTo(outputDir)
    }

    operations.map { OperationTypeSpecBuilder(it, fragments) }
        .forEach {
          val packageName = it.operation.filePath.formatPackageName()
          val typeSpec = it.toTypeSpec(context.copy())
          JavaFile.builder(packageName, typeSpec).build().writeTo(outputDir)
        }
  }

  private fun List<TypeDeclaration>.supportedTypeDeclarations() =
      filter { it.kind == TypeDeclaration.KIND_ENUM || it.kind == TypeDeclaration.KIND_INPUT_OBJECT_TYPE }

  private fun Map<String, String>.supportedScalarTypeMapping(typeDeclarations: List<TypeDeclaration>) =
      typeDeclarations.filter { it.kind == TypeDeclaration.KIND_SCALAR_TYPE }
          .associate { it.name to (this[it.name] ?: "Object") }

  companion object {
    const val FILE_EXTENSION = "graphql"
    val OUTPUT_DIRECTORY = listOf("generated", "source", "apollo")
    const val APOLLOCODEGEN_VERSION = "0.10.13"
  }

  data class Arguments(
      val irFile: File,
      val outputDir: File,
      val customTypeMap: Map<String, String>,
      val nullableValueType: NullableValueType,
      val generateAccessors: Boolean)
}
