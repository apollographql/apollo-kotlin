package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.KotlinOperationsCodegenOptions
import com.apollographql.apollo3.compiler.KotlinSchemaCodegenOptions
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.Transform
import com.apollographql.apollo3.compiler.codegen.ExecutableSchemaLayout
import com.apollographql.apollo3.compiler.codegen.OperationsLayout
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.SchemaLayout
import com.apollographql.apollo3.compiler.codegen.kotlin.executableschema.AdapterRegistryBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.executableschema.ExecutableSchemaBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.executableschema.MainResolverBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.helpers.addInternal
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.FragmentBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.FragmentModelsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.FragmentResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.FragmentSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.FragmentVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.operations.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.CustomScalarAdaptersBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.EnumAsEnumBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.EnumAsSealedBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.EnumResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.InputObjectAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.InputObjectBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.InterfaceBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.PaginationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.ScalarBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.schema.UnionBuilder
import com.apollographql.apollo3.compiler.defaultAddDefaultArgumentForInputObjects
import com.apollographql.apollo3.compiler.defaultAddJvmOverloads
import com.apollographql.apollo3.compiler.defaultAddUnkownForEnums
import com.apollographql.apollo3.compiler.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.defaultGenerateInputBuilders
import com.apollographql.apollo3.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.defaultGenerateSchema
import com.apollographql.apollo3.compiler.defaultJsExport
import com.apollographql.apollo3.compiler.defaultRequiresOptInAnnotation
import com.apollographql.apollo3.compiler.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo3.compiler.generateMethodsKotlin
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.maybeTransform
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.kotlinpoet.FileSpec

private class OutputBuilder {
  val builders: MutableList<CgFileBuilder> = mutableListOf()
}

private fun buildOutput(
    codegenSchema: CodegenSchema,
    upstreamCodegenMetadatas: List<CodegenMetadata>,
    requiresOptInAnnotation: String?,
    targetLanguage: TargetLanguage,
    kotlinOutputTransform: Transform<KotlinOutput>?,
    generateAsInternal: Boolean,
    block: OutputBuilder.(KotlinResolver) -> Unit,
): KotlinOutput {

  upstreamCodegenMetadatas.forEach {
    check(it.targetLanguage == targetLanguage) {
      "Apollo: Cannot depend on '${it.targetLanguage}' generated models (expected: '$targetLanguage')."
    }
  }
  val resolver = KotlinResolver(
      entries = upstreamCodegenMetadatas.flatMap { it.entries },
      next = null,
      scalarMapping = codegenSchema.scalarMapping,
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
      codegenMetadata = CodegenMetadata(targetLanguage = targetLanguage, entries = resolver.entries().sortedBy { it.key.id })
  ).maybeTransform(kotlinOutputTransform)
}

internal object KotlinCodegen {
  fun buildSchemaSources(
      codegenSchema: CodegenSchema,
      targetLanguage: TargetLanguage,
      irSchema: IrSchema?,
      codegenOptions: KotlinSchemaCodegenOptions,
      layout: SchemaLayout,
      kotlinOutputTransform: Transform<KotlinOutput>?,
  ): KotlinOutput {
    check(irSchema is DefaultIrSchema)

    val generateDataBuilders = codegenSchema.generateDataBuilders
    val generateMethods = generateMethodsKotlin(codegenOptions.generateMethods)
    val generateSchema = codegenOptions.generateSchema ?: defaultGenerateSchema || generateDataBuilders

    val generateAsInternal = codegenOptions.generateAsInternal ?: defaultGenerateAsInternal
    val generateInputBuilders = codegenOptions.generateInputBuilders ?: defaultGenerateInputBuilders
    val jsExport = codegenOptions.jsExport ?: defaultJsExport
    val requiresOptInAnnotation = codegenOptions.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation
    val sealedClassesForEnumsMatching = codegenOptions.sealedClassesForEnumsMatching ?: defaultSealedClassesForEnumsMatching
    val addUnknownForEnums = codegenOptions.addUnknownForEnums ?: defaultAddUnkownForEnums
    val addDefaultArgumentForInputObjects = codegenOptions.addDefaultArgumentForInputObjects
        ?: defaultAddDefaultArgumentForInputObjects

    val scalarMapping = codegenSchema.scalarMapping

    return buildOutput(
        codegenSchema = codegenSchema,
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
        builders.add(ScalarBuilder(context, irScalar, scalarMapping.get(irScalar.name)?.targetName))
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
        builders.add(UnionBuilder(context, irUnion, generateDataBuilders))
      }
      irSchema.irInterfaces.forEach { irInterface ->
        builders.add(InterfaceBuilder(context, irInterface, generateDataBuilders))
      }
      irSchema.irObjects.forEach { irObject ->
        builders.add(ObjectBuilder(context, irObject, generateDataBuilders))
      }
      if (generateSchema && context.resolver.resolve(ResolverKey(ResolverKeyKind.Schema, "")) == null) {
        builders.add(SchemaBuilder(context, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions, irSchema.irEnums))
        builders.add(CustomScalarAdaptersBuilder(context, scalarMapping))
      }

      if (irSchema.connectionTypes.isNotEmpty() && context.resolver.resolve(ResolverKey(ResolverKeyKind.Pagination, "")) == null) {
        builders.add(PaginationBuilder(context, irSchema.connectionTypes))
      }
    }
  }

  fun buildOperationSources(
      codegenSchema: CodegenSchema,
      targetLanguage: TargetLanguage,
      irOperations: IrOperations,
      operationOutput: OperationOutput,
      upstreamCodegenMetadata: List<CodegenMetadata>,
      codegenOptions: KotlinOperationsCodegenOptions,
      layout: OperationsLayout,
      kotlinOutputTransform: Transform<KotlinOutput>?,
  ): KotlinOutput {
    val generateDataBuilders = codegenSchema.generateDataBuilders
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
        codegenSchema = codegenSchema,
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
                      generateDataBuilders,
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
                    generateDataBuilders,
                    generateInputBuilders
                )
            )
          }
    }
  }

  fun buildExecutableSchema(
      codegenSchema: CodegenSchema,
      codegenMetadata: CodegenMetadata,
      irTargetObjects: List<IrTargetObject>,
      layout: ExecutableSchemaLayout,
      serviceName: String,
  ): KotlinOutput {
    val targetLanguage = TargetLanguage.KOTLIN_1_9
    return buildOutput(
        codegenSchema,
        listOf(codegenMetadata),
        null,
        targetLanguage,
        null,
        true,
    ) { resolver ->
      val context = KotlinExecutableSchemaContext(
          generateMethods = emptyList(),
          jsExport = false,
          layout = layout,
          resolver = resolver,
          targetLanguage = targetLanguage,
      )

      val mainResolverBuilder = MainResolverBuilder(
          context = context,
          serviceName = serviceName,
          irTargetObjects = irTargetObjects
      )
      builders.add(mainResolverBuilder)

      val adapterRegistryBuilder = AdapterRegistryBuilder(
          context = context,
          serviceName = serviceName,
          codegenSchema = codegenSchema
      )
      builders.add(adapterRegistryBuilder)

      builders.add(
          ExecutableSchemaBuilderBuilder(
              context = context,
              serviceName = serviceName,
              mainResolver = mainResolverBuilder.className,
              adapterRegistry = adapterRegistryBuilder.memberName,
              irTargetObjects = irTargetObjects
          )
      )
    }
  }
}

