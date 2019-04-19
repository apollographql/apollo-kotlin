package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ClassNames
import com.apollographql.apollo.compiler.ast.toAST
import com.apollographql.apollo.compiler.formatPackageName
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.TypeDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.moshi.Moshi
import java.io.File

class GraphQLKompiler(
    private val irFile: File,
    private val customTypeMap: Map<String, String>,
    private val outputPackageName: String?,
    private val useSemanticNaming: Boolean
) {
  private val moshi = Moshi.Builder().build()
  private val irAdapter = moshi.adapter(CodeGenerationIR::class.java)

  fun write(outputDir: File) {
    val ir = irAdapter.fromJson(irFile.readText())!!
    val irPackageName = outputPackageName ?: irFile.absolutePath.formatPackageName()
    val fragmentsPackage = if (irPackageName.isNotEmpty()) "$irPackageName.fragment" else "fragment"
    val typesPackageName = if (irPackageName.isNotEmpty()) "$irPackageName.type" else "type"
    val customTypeMap = customTypeMap.supportedTypeMap(ir.typesUsed)
    val ast = ir.toAST(
        customTypeMap = customTypeMap,
        typesPackageName = typesPackageName,
        fragmentsPackage = fragmentsPackage,
        useSemanticNaming = useSemanticNaming
    )

    if (irPackageName.isNotEmpty()) {
      File(outputDir, irPackageName.replace('.', File.separatorChar)).deleteRecursively()
    }

    if (ast.customTypes.isNotEmpty()) {
      KotlinCodeGen.customScalarTypeSpec(ast.customTypes).writeTo(outputDir, typesPackageName)
    }

    if (ast.enums.isNotEmpty()) {
      ast.enums.map { KotlinCodeGen.enumTypeSpec(it) }.forEach { it.writeTo(outputDir, typesPackageName) }
    }

    if (ast.inputTypes.isNotEmpty()) {
      ast.inputTypes.map { KotlinCodeGen.inputTypeSpec(it) }.forEach { it.writeTo(outputDir, typesPackageName) }
    }

    if (ast.fragments.isNotEmpty()) {
      ast.fragments.map { KotlinCodeGen.fragmentTypeSpec(it) }.forEach { it.writeTo(outputDir, fragmentsPackage) }
    }

    if (ast.operations.isNotEmpty()) {
      ast.operations.forEach { operation ->
        val targetPackage = outputPackageName ?: operation.filePath.formatPackageName()
        KotlinCodeGen.operationTypeSpec(operationType = operation, targetPackage = targetPackage).writeTo(
            outputDir = outputDir,
            packageName = targetPackage
        )
      }
    }
  }

  private fun TypeSpec.writeTo(outputDir: File, packageName: String) {
    FileSpec.builder(packageName, name!!)
        .addType(this)
        .build()
        .writeTo(outputDir)
  }

  private fun Map<String, String>.supportedTypeMap(typeDeclarations: List<TypeDeclaration>): Map<String, String> {
    val idScalarTypeMap = ScalarType.ID.name to (this[ScalarType.ID.name] ?: String::class.asClassName().toString())
    return typeDeclarations.filter { it.kind == TypeDeclaration.KIND_SCALAR_TYPE }
        .associate { it.name to (this[it.name] ?: ClassNames.OBJECT.toString()) }
        .plus(idScalarTypeMap)
  }
}