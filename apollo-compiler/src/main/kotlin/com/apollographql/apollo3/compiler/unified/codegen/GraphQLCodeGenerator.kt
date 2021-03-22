package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.VERSION
import com.apollographql.apollo3.compiler.backend.ast.AstBuilder.Companion.buildAst
import com.apollographql.apollo3.compiler.backend.codegen.adapterPackageName
import com.apollographql.apollo3.compiler.backend.codegen.implementationTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.inputObjectAdapterTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.interfaceTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.kotlinNameForEnum
import com.apollographql.apollo3.compiler.backend.codegen.patchKotlinNativeOptionalArrayProperties
import com.apollographql.apollo3.compiler.backend.codegen.responseAdapterTypeSpec
import com.apollographql.apollo3.compiler.backend.codegen.typeSpec
import com.apollographql.apollo3.compiler.backend.codegen.typeSpecs
import com.apollographql.apollo3.compiler.backend.codegen.variablesAdapterTypeSpec
import com.apollographql.apollo3.compiler.backend.ir.BackendIr
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.toIntrospectionSchema
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.unified.IntermediateRepresentation
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

class GraphQLCodeGenerator(
    private val ir: IntermediateRepresentation,
    private val generateAsInternal: Boolean = false,
    private val enumAsSealedClassPatternFilters: List<Regex>,
    private val generateScalarMapping: Boolean,
) {
  fun write(outputDir: File) {

    if (generateScalarMapping) {
      ir.customScalars.typeSpec()
          .toFileSpec(ir.customScalars.packageName)
          .writeTo(outputDir)
    }

    ir.enums
        .forEach { enum ->
          fileSpecBuilder(enum.packageName, kotlinNameForEnum(enum.name))
              .apply {
                enum.typeSpecs(
                    enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters,
                    packageName = enum.packageName
                ).forEach {
                  addType(it.internal(generateAsInternal))
                }
              }.build()
              .writeTo(outputDir)
        }

    ir.inputObjects
        .forEach { inputType ->
          inputType.typeSpec()
              .toFileSpec(inputType.packageName)
              .writeTo(outputDir)
          inputType.adapterTypeSpec()
              .toFileSpec(adapterPackageName(inputType.packageName))
              .writeTo(outputDir)
        }

//    ir.allNamedFragments
//        .filter {  }
//        .forEach { fragmentType ->
//          if (generateFragmentsAsInterfaces) {
//            fragmentType
//                .interfaceTypeSpec()
//                .toFileSpec(fragmentsPackageName)
//                .writeTo(outputDir)
//          }
//
//          if (generateFragmentImplementations || !generateFragmentsAsInterfaces) {
//            fragmentType
//                .implementationTypeSpec(generateFragmentsAsInterfaces)
//                .toFileSpec(fragmentsPackageName)
//                .writeTo(outputDir)
//
//            fragmentType.variables.variablesAdapterTypeSpec(fragmentsPackageName, fragmentType.implementationType.name)
//                .toFileSpec("${fragmentsPackageName}.adapter")
//                .writeTo(outputDir)
//
//            fragmentType
//                .responseAdapterTypeSpec(generateFragmentsAsInterfaces)
//                .toFileSpec("${fragmentsPackageName}.adapter")
//                .writeTo(outputDir)
//          }
//        }

    ir.operations.forEach { operation ->
      operation.typeSpec()
          .toFileSpec(operation.packageName)
          .writeTo(outputDir)

//      operation.variablesAdapterTypeSpec(operation.packageName, operation.name)
//          .toFileSpec("${packageName}.adapter")
//          .writeTo(outputDir)
//
//      operation.responseAdapterTypeSpec(generateFragmentsAsInterfaces)
//          .toFileSpec("${packageName}.adapter")
//          .writeTo(outputDir)
    }
  }

  /**
   * Generates a file with the name of this type and the specified package name
   */
  private fun TypeSpec.toFileSpec(packageName: String) = fileSpecBuilder(packageName, name!!)
      .addType(this.internal(generateAsInternal))
      .build()

  private fun fileSpecBuilder(packageName: String, name: String): FileSpec.Builder =
      FileSpec
          .builder(packageName, name)
          .addComment("AUTO-GENERATED FILE. DO NOT MODIFY.\n\n" +
              "This class was automatically generated by Apollo GraphQL version '$VERSION'.\n"
          )

  private fun TypeSpec.internal(generateAsInternal: Boolean): TypeSpec {
    return if (generateAsInternal) {
      this.toBuilder().addModifiers(KModifier.INTERNAL).build()
    } else {
      this
    }
  }
}