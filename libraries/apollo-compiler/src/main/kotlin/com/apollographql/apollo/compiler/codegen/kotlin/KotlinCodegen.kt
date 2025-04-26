package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.apollo.compiler.CODEGEN_METADATA_VERSION
import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.CodegenOptions
import com.apollographql.apollo.compiler.KotlinOperationsCodegenOptions
import com.apollographql.apollo.compiler.KotlinSchemaCodegenOptions
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.OperationsLayout
import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.SchemaLayout
import com.apollographql.apollo.compiler.codegen.kotlin.builders.AdaptBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.builders.DataBuilderBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.builders.DataMapBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.builders.ResolverBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.helpers.addInternal
import com.apollographql.apollo.compiler.codegen.kotlin.operations.FragmentBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.FragmentModelsBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.FragmentResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.FragmentSelectionsBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.FragmentVariablesAdapterBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.OperationBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.OperationResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.OperationSelectionsBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.operations.OperationVariablesAdapterBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.CustomScalarAdaptersBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.EnumAsEnumBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.EnumAsSealedBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.EnumResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.InlineClassBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.InputObjectAdapterBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.InputObjectBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.InterfaceBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.ObjectBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.PaginationBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.ScalarBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.SchemaBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.UnionBuilder
import com.apollographql.apollo.compiler.codegen.kotlin.schema.asTargetClassName
import com.apollographql.apollo.compiler.defaultAddDefaultArgumentForInputObjects
import com.apollographql.apollo.compiler.defaultAddJvmOverloads
import com.apollographql.apollo.compiler.defaultAddUnkownForEnums
import com.apollographql.apollo.compiler.defaultGenerateAsInternal
import com.apollographql.apollo.compiler.defaultGenerateFilterNotNull
import com.apollographql.apollo.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo.compiler.defaultGenerateInputBuilders
import com.apollographql.apollo.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo.compiler.defaultGenerateSchema
import com.apollographql.apollo.compiler.defaultJsExport
import com.apollographql.apollo.compiler.defaultRequiresOptInAnnotation
import com.apollographql.apollo.compiler.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo.compiler.generateMethodsKotlin
import com.apollographql.apollo.compiler.ir.DefaultIrDataBuilders
import com.apollographql.apollo.compiler.ir.DefaultIrSchema
import com.apollographql.apollo.compiler.ir.IrDataBuilders
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.ir.IrSchema
import com.apollographql.apollo.compiler.maybeTransform
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.findOperationId
import com.squareup.kotlinpoet.FileSpec

private class OutputBuilder {
  val builders: MutableList<CgFileBuilder> = mutableListOf()
}

private fun buildOutput(
    upstreamCodegenMetadatas: List<CodegenMetadata>,
    requiresOptInAnnotation: String?,
    targetLanguage: TargetLanguage,
    kotlinOutputTransform: Transform<KotlinOutput>?,
    generateAsInternal: Boolean,
    block: OutputBuilder.(KotlinResolver) -> Unit,
): KotlinOutput {

  val upstreamCodegenMetadata = upstreamCodegenMetadatas.fold(CodegenMetadata(
      version = CODEGEN_METADATA_VERSION,
      targetLanguage = targetLanguage,
      emptyList(),
      emptyMap(),
      emptyMap(),
      emptyMap(),
      emptyMap()
  )) { acc, metadata ->
    acc + metadata
  }
  val resolver = KotlinResolver(
      upstreamCodegenMetadata = upstreamCodegenMetadata,
      requiresOptInAnnotation = requiresOptInAnnotation,
  )

  val outputBuilder = OutputBuilder()

  outputBuilder.block(resolver)

  /**
   * 1st pass: call prepare on all builders
   */
  outputBuilder.builders.forEach { it.prepare() }

  /**
   * 2nd pass: build the [CgFile]s
   */
  val fileSpecs = outputBuilder.builders
      .map {
        val cgFile = it.build()
        val builder = FileSpec.builder(
            packageName = cgFile.packageName,
            fileName = cgFile.fileName
        ).addFileComment(
            """
                
                AUTO-GENERATED FILE. DO NOT MODIFY.
                
                This class was automatically generated by Apollo GraphQL version '$APOLLO_VERSION'.
                
              """.trimIndent()
        )

        cgFile.typeSpecs.forEach { typeSpec ->
          builder.addType(typeSpec)
        }
        cgFile.funSpecs.forEach { funSpec ->
          builder.addFunction(funSpec)
        }
        cgFile.propertySpecs.forEach { propertySpec ->
          builder.addProperty(propertySpec)
        }
        cgFile.imports.forEach {
          builder.addAliasedImport(it.className, it.alias)
        }
        if (generateAsInternal) {
          builder.addInternal(listOf(".*"))
        }
        builder.build()
      }
  return KotlinOutput(
      fileSpecs = fileSpecs,
      codegenMetadata = resolver.toCodegenMetadata(targetLanguage = targetLanguage)
  ).maybeTransform(kotlinOutputTransform)
}

internal object KotlinCodegen {
  fun buildSchemaSources(
      targetLanguage: TargetLanguage,
      irSchema: IrSchema?,
      codegenOptions: KotlinSchemaCodegenOptions,
      layout: SchemaLayout,
      kotlinOutputTransform: Transform<KotlinOutput>?,
  ): KotlinOutput {
    check(irSchema is DefaultIrSchema)

    val generateMethods = generateMethodsKotlin(codegenOptions.generateMethods)
    val generateSchema = codegenOptions.generateSchema ?: defaultGenerateSchema

    val generateAsInternal = codegenOptions.generateAsInternal ?: defaultGenerateAsInternal
    val generateInputBuilders = codegenOptions.generateInputBuilders ?: defaultGenerateInputBuilders
    val jsExport = codegenOptions.jsExport ?: defaultJsExport
    val requiresOptInAnnotation = codegenOptions.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation
    val sealedClassesForEnumsMatching = codegenOptions.sealedClassesForEnumsMatching ?: defaultSealedClassesForEnumsMatching
    val addUnknownForEnums = codegenOptions.addUnknownForEnums ?: defaultAddUnkownForEnums
    val addDefaultArgumentForInputObjects = codegenOptions.addDefaultArgumentForInputObjects
        ?: defaultAddDefaultArgumentForInputObjects

    return buildOutput(
        upstreamCodegenMetadatas = emptyList(),
        requiresOptInAnnotation = requiresOptInAnnotation,
        targetLanguage = targetLanguage,
        kotlinOutputTransform = kotlinOutputTransform,
        generateAsInternal = generateAsInternal,
    ) { resolver ->
      val context = KotlinSchemaContext(
          generateMethods = generateMethods,
          jsExport = jsExport,
          layout = layout,
          resolver = resolver,
          targetLanguage = targetLanguage,
      )

      irSchema.irScalars.forEach { irScalar ->
        var inlineClassBuilder: InlineClassBuilder? = null
        if (irScalar.mapToBuiltIn?.inline == true) {
          inlineClassBuilder = InlineClassBuilder(context, irScalar, irScalar.mapToBuiltIn.builtIn.asTargetClassName())
          builders.add(inlineClassBuilder)
        }
        builders.add(ScalarBuilder(context, irScalar, inlineClassBuilder?.className))
      }
      irSchema.irEnums.forEach { irEnum ->
        if (sealedClassesForEnumsMatching.any { Regex(it).matches(irEnum.name) }) {
          builders.add(EnumAsSealedBuilder(context, irEnum))
        } else {
          builders.add(EnumAsEnumBuilder(context, irEnum, addUnknownForEnums))
        }
        builders.add(EnumResponseAdapterBuilder(context, irEnum))
      }
      irSchema.irInputObjects.forEach { irInputObject ->
        builders.add(InputObjectBuilder(context, irInputObject, generateInputBuilders, addDefaultArgumentForInputObjects))
        builders.add(InputObjectAdapterBuilder(context, irInputObject))
      }
      irSchema.irUnions.forEach { irUnion ->
        builders.add(UnionBuilder(context, irUnion))
      }
      irSchema.irInterfaces.forEach { irInterface ->
        builders.add(InterfaceBuilder(context, irInterface))
      }
      irSchema.irObjects.forEach { irObject ->
        builders.add(ObjectBuilder(context, irObject))
      }
      if (generateSchema && context.resolver.resolve(ResolverKey(ResolverKeyKind.Schema, "")) == null) {
        builders.add(SchemaBuilder(context, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions, irSchema.irEnums))
        builders.add(CustomScalarAdaptersBuilder(context, irSchema.irScalars))
      }

      if (irSchema.connectionTypes.isNotEmpty() && context.resolver.resolve(ResolverKey(ResolverKeyKind.Pagination, "")) == null) {
        builders.add(PaginationBuilder(context, irSchema.connectionTypes))
      }
    }
  }

  fun buildOperationSources(
      targetLanguage: TargetLanguage,
      irOperations: IrOperations,
      operationOutput: OperationOutput,
      upstreamCodegenMetadata: List<CodegenMetadata>,
      codegenOptions: KotlinOperationsCodegenOptions,
      layout: OperationsLayout,
      kotlinOutputTransform: Transform<KotlinOutput>?,
  ): KotlinOutput {
    val flatten = irOperations.flattenModels

    val generateFragmentImplementations = codegenOptions.generateFragmentImplementations ?: defaultGenerateFragmentImplementations
    val generateMethods = generateMethodsKotlin(codegenOptions.generateMethods)
    val generateQueryDocument = codegenOptions.generateQueryDocument ?: defaultGenerateQueryDocument

    val addJvmOverloads = codegenOptions.addJvmOverloads ?: defaultAddJvmOverloads
    val generateAsInternal = codegenOptions.generateAsInternal ?: defaultGenerateAsInternal
    val generateFilterNotNull = codegenOptions.generateFilterNotNull ?: defaultGenerateFilterNotNull
    val generateInputBuilders = codegenOptions.generateInputBuilders ?: defaultGenerateInputBuilders
    val jsExport = codegenOptions.jsExport ?: defaultJsExport
    val requiresOptInAnnotation = codegenOptions.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation

    return buildOutput(
        upstreamCodegenMetadatas = upstreamCodegenMetadata,
        requiresOptInAnnotation = requiresOptInAnnotation,
        targetLanguage = targetLanguage,
        kotlinOutputTransform = kotlinOutputTransform,
        generateAsInternal = generateAsInternal
    ) { resolver ->
      val context = KotlinOperationsContext(
          generateMethods = generateMethods,
          jsExport = jsExport,
          layout = layout,
          resolver = resolver,
          targetLanguage = targetLanguage,
      )

      irOperations.fragments
          .forEach { fragment ->
            builders.add(
                FragmentModelsBuilder(
                    context,
                    fragment,
                    (fragment.interfaceModelGroup ?: fragment.dataModelGroup),
                    fragment.interfaceModelGroup == null,
                    flatten,
                )
            )

            builders.add(FragmentSelectionsBuilder(context, fragment))

            if (generateFragmentImplementations || fragment.interfaceModelGroup == null) {
              builders.add(FragmentResponseAdapterBuilder(context, fragment, flatten))
            }

            if (generateFragmentImplementations) {
              builders.add(
                  FragmentBuilder(
                      context,
                      generateFilterNotNull,
                      fragment,
                      flatten,
                      addJvmOverloads,
                      generateInputBuilders,
                  )
              )
              if (fragment.variables.isNotEmpty()) {
                builders.add(FragmentVariablesAdapterBuilder(context, fragment))
              }
            }
          }

      irOperations.operations
          .forEach { operation ->
            if (operation.variables.isNotEmpty()) {
              builders.add(OperationVariablesAdapterBuilder(context, operation))
            }

            builders.add(OperationSelectionsBuilder(context, operation))
            builders.add(OperationResponseAdapterBuilder(context, operation, flatten))

            builders.add(
                OperationBuilder(
                    context,
                    generateFilterNotNull,
                    operationOutput.findOperationId(operation.name),
                    generateQueryDocument,
                    operation,
                    flatten,
                    addJvmOverloads,
                    generateInputBuilders
                )
            )
          }
    }
  }

  fun buildDataBuilders(
      dataBuilders: IrDataBuilders,
      layout: SchemaLayout,
      codegenOptions: CodegenOptions,
      upstreamCodegenMetadata: List<CodegenMetadata>,
      targetLanguage: TargetLanguage,
  ): KotlinOutput {
    check(dataBuilders is DefaultIrDataBuilders)

    val generateMethods = generateMethodsKotlin(codegenOptions.generateMethods)
    val generateAsInternal = codegenOptions.generateAsInternal ?: defaultGenerateAsInternal
    val jsExport = codegenOptions.jsExport ?: defaultJsExport
    val requiresOptInAnnotation = codegenOptions.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation

    return buildOutput(
        upstreamCodegenMetadatas = upstreamCodegenMetadata,
        requiresOptInAnnotation = requiresOptInAnnotation,
        targetLanguage = targetLanguage,
        kotlinOutputTransform = null,
        generateAsInternal = generateAsInternal
    ) { resolver ->
      val context = KotlinDataBuilderContext(
          generateMethods = generateMethods,
          jsExport = jsExport,
          layout = layout,
          resolver = resolver,
          targetLanguage = targetLanguage,
      )

      dataBuilders.dataBuilders.forEach {
        builders.add(DataBuilderBuilder(context, it))
        builders.add(DataMapBuilder(context, it, withFields = true))
        if (it.isAbstract) {
          builders.add(DataMapBuilder(context, it, withFields = false))
        }
        builders.add(ResolverBuilder(context, dataBuilders.possibleTypes))
        builders.add(AdaptBuilder(context, dataBuilders.scalars))
      }
    }
  }
}
