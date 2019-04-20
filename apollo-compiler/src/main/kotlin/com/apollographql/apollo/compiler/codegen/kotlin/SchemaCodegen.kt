package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.*
import com.apollographql.apollo.compiler.formatPackageName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal class SchemaCodegen(
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val outputPackageName: String?
) : SchemaVisitor {
  private var fileSpecs: List<FileSpec> = emptyList()

  override fun visit(customTypes: CustomTypes) {
    fileSpecs = fileSpecs + customTypes.typeSpec().fileSpec(typesPackageName)
  }

  override fun visit(enumType: EnumType) {
    fileSpecs = fileSpecs + enumType.typeSpec().fileSpec(typesPackageName)
  }

  override fun visit(inputType: InputType) {
    fileSpecs = fileSpecs + inputType.typeSpec().fileSpec(typesPackageName)
  }

  override fun visit(fragmentType: FragmentType) {
    fileSpecs = fileSpecs + fragmentType.typeSpec().fileSpec(fragmentsPackage)
  }

  override fun visit(operationType: OperationType) {
    val targetPackage = outputPackageName ?: operationType.filePath.formatPackageName()
    fileSpecs = fileSpecs + operationType.typeSpec(targetPackage).fileSpec(targetPackage)
  }

  fun writeTo(outputDir: File) {
    fileSpecs.forEach { it.writeTo(outputDir) }
  }

  private fun TypeSpec.fileSpec(packageName: String) =
      FileSpec
          .builder(packageName, name!!)
          .addType(this)
          .build()
}