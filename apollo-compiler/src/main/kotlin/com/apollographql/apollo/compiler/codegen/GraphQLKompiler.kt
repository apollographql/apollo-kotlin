package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.compiler.ast.buildCodeGenerationAst
import com.apollographql.apollo.compiler.frontend.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

class GraphQLKompiler(
    private val ir: CodeGenerationIR,
    private val schema: IntrospectionSchema,
    private val customTypeMap: Map<String, String>,
    private val useSemanticNaming: Boolean,
    private val generateAsInternal: Boolean = false,
    private val operationOutput: OperationOutput,
    private val generateFilterNotNull: Boolean,
    private val enumAsSealedClassPatternFilters: List<Regex>
) {
  fun write(outputDir: File) {
    val ast = ir.buildCodeGenerationAst(
        schema = schema,
        customTypeMap = customTypeMap,
        operationOutput = operationOutput,
        useSemanticNaming = useSemanticNaming,
        typesPackageName = ir.typesPackageName,
        fragmentsPackage = ir.fragmentsPackageName
    )

    ast.customTypes
        .filterKeys {
          ir.scalarsToGenerate.contains(it)
        }.takeIf {
          /**
           * Skip generating the ScalarType enum if it's empty
           * This happens in multi-module for leaf modules
           */
          it.isNotEmpty()
        }?.typeSpec(generateAsInternal)
        ?.fileSpec(ir.typesPackageName)
        ?.writeTo(outputDir)

    ast.enumTypes
        .filter { ir.enumsToGenerate.contains(it.graphqlName) }
        .forEach { enumType ->
          enumType
              .typeSpec(
                  generateAsInternal = generateAsInternal,
                  enumAsSealedClassPatternFilters = enumAsSealedClassPatternFilters
              )
              .fileSpec(ir.typesPackageName)
              .writeTo(outputDir)
        }

    ast.inputTypes
        .filter { ir.inputObjectsToGenerate.contains(it.graphqlName) }
        .forEach { inputType ->
          inputType
              .typeSpec(generateAsInternal)
              .fileSpec(ir.typesPackageName)
              .writeTo(outputDir)
        }

    ast.fragmentTypes
        .filter { ir.fragmentsToGenerate.contains(it.graphqlName) }
        .forEach { fragmentType ->
          fragmentType
              .typeSpec(generateAsInternal)
              .fileSpec(ir.fragmentsPackageName)
              .writeTo(outputDir)
        }

    ast.fragmentTypes
        .filter { ir.fragmentsToGenerate.contains(it.graphqlName) }
        .forEach { fragmentType ->
          fragmentType
              .responseAdapterTypeSpec(generateAsInternal)
              .fileSpec(ir.fragmentsPackageName)
              .writeTo(outputDir)
        }

    ast.operationTypes.forEach { operationType ->
      operationType
          .typeSpec(
              targetPackage = operationType.packageName,
              generateAsInternal = generateAsInternal
          )
          .let {
            if (generateFilterNotNull) {
              it.patchKotlinNativeOptionalArrayProperties()
            } else it
          }
          .fileSpec(operationType.packageName)
          .writeTo(outputDir)
    }

    ast.operationTypes.forEach { operationType ->
      operationType.responseAdapterTypeSpec(generateAsInternal)
          .fileSpec(operationType.packageName)
          .writeTo(outputDir)
    }
  }

  private fun TypeSpec.fileSpec(packageName: String) =
      FileSpec
          .builder(packageName, name!!)
          .addType(this)
          .addComment("AUTO-GENERATED FILE. DO NOT MODIFY.\n\n" +
              "This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.\n" +
              "It should not be modified by hand.\n"
          )
          .build()
}
