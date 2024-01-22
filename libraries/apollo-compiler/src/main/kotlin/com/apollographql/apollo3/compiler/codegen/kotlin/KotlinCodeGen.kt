package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.KotlinOperationsCodegenOptions
import com.apollographql.apollo3.compiler.KotlinOutput
import com.apollographql.apollo3.compiler.KotlinResolverCodegenOptions
import com.apollographql.apollo3.compiler.KotlinSchemaCodegenOptions
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.allTypes
import com.apollographql.apollo3.compiler.codegen.CodegenSymbols
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.kotlin.file.AdapterRegistryBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.CustomScalarAdaptersBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.EnumAsEnumBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.EnumAsSealedBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.EnumResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.FragmentBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.FragmentModelsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.FragmentResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.FragmentSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.FragmentVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.InputObjectAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.InputObjectBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.InterfaceBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.MainResolverBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.PaginationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.ScalarBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.UnionBuilder
import com.apollographql.apollo3.compiler.defaultAddJvmOverloads
import com.apollographql.apollo3.compiler.defaultDecapitalizeFields
import com.apollographql.apollo3.compiler.defaultGenerateAsInternal
import com.apollographql.apollo3.compiler.defaultGenerateFilterNotNull
import com.apollographql.apollo3.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo3.compiler.defaultGenerateInputBuilders
import com.apollographql.apollo3.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo3.compiler.defaultGenerateSchema
import com.apollographql.apollo3.compiler.defaultGeneratedSchemaName
import com.apollographql.apollo3.compiler.defaultJsExport
import com.apollographql.apollo3.compiler.defaultRequiresOptInAnnotation
import com.apollographql.apollo3.compiler.defaultSealedClassesForEnumsMatching
import com.apollographql.apollo3.compiler.defaultTargetLanguage
import com.apollographql.apollo3.compiler.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.generateMethodsKotlin
import com.apollographql.apollo3.compiler.hooks.internal.addInternal
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.squareup.kotlinpoet.FileSpec

object KotlinCodegen {
  private class OutputBuilder {
    val builders: MutableList<CgFileBuilder> = mutableListOf()
  }

  private fun buildOutput(
      codegenSchema: CodegenSchema,
      upstreamCodegenSymbols: List<CodegenSymbols>,
      requiresOptInAnnotation: String?,
      generateAsInternal: Boolean,
      block: OutputBuilder.(KotlinResolver) -> Unit,
  ): KotlinOutput {
    val upstreamResolver = upstreamCodegenSymbols.fold(null as KotlinResolver?) { acc, resolverInfo ->
      KotlinResolver(resolverInfo.entries, acc, codegenSchema.scalarMapping, requiresOptInAnnotation)
    }
    val resolver = KotlinResolver(
        entries = emptyList(),
        next = upstreamResolver,
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
     * 2nd pass: build the [CgFile]s and go through hooks
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
          builder.build()
        }.let {
          if (generateAsInternal) {
            addInternal(it, setOf(".*"))
          } else {
            it
          }
        }

    return KotlinOutput(
        fileSpecs = fileSpecs,
        symbols = CodegenSymbols(entries = resolver.entries())
    )
  }


  fun buildSchema(
      codegenSchema: CodegenSchema,
      irSchema: IrSchema,
      options: KotlinSchemaCodegenOptions,
  ): KotlinOutput {
    check(irSchema is DefaultIrSchema)
    val generateSchema = options.generateSchema ?: defaultGenerateSchema || codegenSchema.generateDataBuilders
    val generatedSchemaName = options.generatedSchemaName ?: defaultGeneratedSchemaName
    val scalarMapping = codegenSchema.scalarMapping
    val sealedClassesForEnumsMatching = options.sealedClassesForEnumsMatching ?: defaultSealedClassesForEnumsMatching
    val generateInputBuilders = options.generateInputBuilders ?: defaultGenerateInputBuilders
    val generateDataBuilders = codegenSchema.generateDataBuilders
    val requiresOptInAnnotation = options.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation
    val generateAsInternal = options.generateAsInternal ?: defaultGenerateAsInternal
    val jsExport = options.jsExport ?: defaultJsExport
    val decapitalizeFields = options.decapitalizeFields ?: defaultDecapitalizeFields

    return buildOutput(
        codegenSchema,
        emptyList(),
        requiresOptInAnnotation,
        generateAsInternal
    ) { resolver ->
      val layout = KotlinSchemaCodegenLayout(
          allTypes = codegenSchema.allTypes(),
          schemaPackageName = codegenSchema.packageName,
      )

      val context = KotlinSchemaContext(
          generateMethods = generateMethodsKotlin(options.generateMethods),
          jsExport = jsExport,
          layout = layout,
          resolver = resolver,
          targetLanguageVersion = options.targetLanguage ?: defaultTargetLanguage,
          decapitalizeFields = decapitalizeFields
      )

      irSchema.irScalars.forEach { irScalar ->
        builders.add(ScalarBuilder(context, irScalar, scalarMapping.get(irScalar.name)?.targetName))
      }
      irSchema.irEnums.forEach { irEnum ->
        if (sealedClassesForEnumsMatching.any { Regex(it).matches(irEnum.name) }) {
          builders.add(EnumAsSealedBuilder(context, irEnum))
        } else {
          builders.add(EnumAsEnumBuilder(context, irEnum, true))
        }
        builders.add(EnumResponseAdapterBuilder(context, irEnum))
      }
      irSchema.irInputObjects.forEach { irInputObject ->
        builders.add(InputObjectBuilder(context, irInputObject, generateInputBuilders, true))
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
        builders.add(SchemaBuilder(context, generatedSchemaName, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions, irSchema.irEnums))
        builders.add(CustomScalarAdaptersBuilder(context, scalarMapping))
      }

      if (irSchema.connectionTypes.isNotEmpty() && context.resolver.resolve(ResolverKey(ResolverKeyKind.Pagination, "")) == null) {
        builders.add(PaginationBuilder(context, irSchema.connectionTypes))
      }
    }
  }

  fun buildOperations(
      codegenSchema: CodegenSchema,
      irOperations: IrOperations,
      upstreamCodegenSymbols: List<CodegenSymbols>,
      codegenOptions: KotlinOperationsCodegenOptions,
      packageNameGenerator: PackageNameGenerator,
  ): KotlinOutput {
    check(irOperations is DefaultIrOperations)

    val generateDataBuilders = irOperations.generateDataBuilders
    val flatten = irOperations.flattenModels
    val decapitalizeFields = irOperations.decapitalizeFields

    val generateFragmentImplementations = codegenOptions.generateFragmentImplementations
        ?: defaultGenerateFragmentImplementations
    val generateMethods = generateMethodsKotlin(codegenOptions.generateMethods)
    val generateQueryDocument = codegenOptions.generateQueryDocument ?: defaultGenerateQueryDocument
    val useSemanticNaming = codegenOptions.useSemanticNaming ?: defaultUseSemanticNaming

    val addJvmOverloads = codegenOptions.addJvmOverloads ?: defaultAddJvmOverloads
    val generateAsInternal = codegenOptions.generateAsInternal ?: defaultGenerateAsInternal
    val generateFilterNotNull = codegenOptions.generateFilterNotNull ?: defaultGenerateFilterNotNull
    val generateInputBuilders = codegenOptions.generateInputBuilders ?: defaultGenerateInputBuilders
    val jsExport = codegenOptions.jsExport ?: defaultJsExport
    val requiresOptInAnnotation = codegenOptions.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation

    val targetLanguageVersion = codegenOptions.targetLanguage ?: defaultTargetLanguage

    return buildOutput(
        codegenSchema,
        upstreamCodegenSymbols,
        requiresOptInAnnotation,
        generateAsInternal,
    ) { resolver ->
      val layout = KotlinOperationsCodegenLayout(
          allTypes = codegenSchema.allTypes(),
          useSemanticNaming = useSemanticNaming,
          packageNameGenerator = packageNameGenerator,
      )

      val context = KotlinOperationsContext(
          generateMethods = generateMethods,
          jsExport = jsExport,
          layout = layout,
          resolver = resolver,
          targetLanguageVersion = targetLanguageVersion,
          decapitalizeFields = decapitalizeFields
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
                    operation.id,
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

  fun buildResolver(
      codegenSchema: CodegenSchema,
      codegenSymbols: CodegenSymbols,
      irTargetObjects: List<IrTargetObject>,
      options: KotlinResolverCodegenOptions,
      packageName: String,
  ): KotlinOutput {
    val requiresOptInAnnotation = options.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation
    val generateAsInternal = options.generateAsInternal ?: defaultGenerateAsInternal
    val decapitalizeFields = options.decapitalizeFields ?: defaultDecapitalizeFields

    return buildOutput(
        codegenSchema,
        listOf(codegenSymbols),
        requiresOptInAnnotation,
        generateAsInternal
    ) { resolver ->
      val layout = KotlinResolverCodegenLayout(
          allTypes = codegenSchema.allTypes(),
          packageName = packageName
      )

      val serviceName = options.serviceName ?: error("Apollo: service name is required")
      val context = KotlinResolverContext(
          generateMethods = emptyList(),
          jsExport = false,
          layout = layout,
          resolver = resolver,
          targetLanguageVersion = options.targetLanguage ?: error("Apollo: targetLanguage is required"),
          decapitalizeFields = decapitalizeFields
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

