package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.ApolloCompiler.clearContents
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.CommonCodegenOptions
import com.apollographql.apollo3.compiler.KotlinCodegenOptions
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
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
import com.apollographql.apollo3.compiler.compilerKotlinHooks
import com.apollographql.apollo3.compiler.defaultAddJvmOverloads
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
import com.apollographql.apollo3.compiler.defaultUseSemanticNaming
import com.apollographql.apollo3.compiler.generateMethodsKotlin
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.ir.toIr
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.apollographql.apollo3.compiler.writeTo
import com.squareup.kotlinpoet.FileSpec
import java.io.File

internal object KotlinCodeGen {
  private fun schemaFileBuilders(
      context: KotlinContext,
      sealedClassesForEnumsMatching: List<String>,
      generateDataBuilders: Boolean,
      generateInputBuilders: Boolean,
      generateSchema: Boolean,
      generatedSchemaName: String,
      scalarMapping: Map<String, ScalarInfo>,
      irSchema: IrSchema,
  ): List<CgFileBuilder> {
    check(irSchema is DefaultIrSchema)

    val builders = mutableListOf<CgFileBuilder>()
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

    return builders
  }

  private fun buildFileSpecs(
      builders: List<CgFileBuilder>,
      hooks: List<ApolloCompilerKotlinHooks>,
  ): List<FileSpec> {
    /**
     * 1st pass: call prepare on all builders
     */
    builders.forEach { it.prepare() }

    /**
     * 2nd pass: build the [CgFile]s and go through hooks
     */
    val fileInfos = builders
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
          ApolloCompilerKotlinHooks.FileInfo(fileSpec = builder.build())
        }
        .let {
          hooks.fold(it as Collection<ApolloCompilerKotlinHooks.FileInfo>) { acc, hooks ->
            hooks.postProcessFiles(acc)
          }
        }

    // Write the files to disk
    return fileInfos.map { it.fileSpec }
  }

  fun writeSchemaAndOperations(
      codegenSchema: CodegenSchema,
      irOperations: IrOperations,
      irSchema: IrSchema?,
      operationOutput: OperationOutput,
      upstreamCodegenMetadata: List<CodegenMetadata>,
      commonCodegenOptions: CommonCodegenOptions,
      kotlinCodegenOptions: KotlinCodegenOptions,
      packageNameGenerator: PackageNameGenerator,
      compilerKotlinHooks: List<ApolloCompilerKotlinHooks>,
      outputDir: File,
      codegenMetadataFile: File?,
  ) {

    outputDir.clearContents()
    check(irOperations is DefaultIrOperations)

    val resolverInfos = upstreamCodegenMetadata.map { it.resolverInfo }

    val generateDataBuilders = irOperations.generateDataBuilders
    val flatten = irOperations.flattenModels
    val decapitalizeFields = irOperations.decapitalizeFields

    val generateFragmentImplementations = commonCodegenOptions.generateFragmentImplementations ?: defaultGenerateFragmentImplementations
    val generateMethods = generateMethodsKotlin(commonCodegenOptions.generateMethods)
    val generateQueryDocument = commonCodegenOptions.generateQueryDocument ?: defaultGenerateQueryDocument
    val generateSchema = commonCodegenOptions.generateSchema ?: defaultGenerateSchema || generateDataBuilders
    val generatedSchemaName = commonCodegenOptions.generatedSchemaName ?: defaultGeneratedSchemaName
    val useSemanticNaming = commonCodegenOptions.useSemanticNaming ?: defaultUseSemanticNaming

    val addJvmOverloads = kotlinCodegenOptions.addJvmOverloads ?: defaultAddJvmOverloads
    val generateAsInternal = kotlinCodegenOptions.generateAsInternal ?: defaultGenerateAsInternal
    val generateFilterNotNull = kotlinCodegenOptions.generateFilterNotNull ?: defaultGenerateFilterNotNull
    val generateInputBuilders = kotlinCodegenOptions.generateInputBuilders ?: defaultGenerateInputBuilders
    val jsExport = kotlinCodegenOptions.jsExport ?: defaultJsExport
    val requiresOptInAnnotation = kotlinCodegenOptions.requiresOptInAnnotation ?: defaultRequiresOptInAnnotation
    val sealedClassesForEnumsMatching = kotlinCodegenOptions.sealedClassesForEnumsMatching ?: defaultSealedClassesForEnumsMatching

    val targetLanguageVersion = codegenSchema.targetLanguage
    val scalarMapping = codegenSchema.scalarMapping
    @Suppress("NAME_SHADOWING")
    val compilerKotlinHooks = compilerKotlinHooks(compilerKotlinHooks, generateAsInternal)

    val upstreamResolver = resolverInfos.fold(null as KotlinResolver?) { acc, resolverInfo ->
      KotlinResolver(resolverInfo.entries, acc, scalarMapping, requiresOptInAnnotation, compilerKotlinHooks)
    }

    val layout = CodegenLayout(
        codegenSchema = codegenSchema,
        packageNameGenerator = packageNameGenerator,
        useSemanticNaming = useSemanticNaming,
        decapitalizeFields = decapitalizeFields,
    )

    val context = KotlinContext(
        generateMethods = generateMethods,
        jsExport = jsExport,
        layout = layout,
        resolver = KotlinResolver(
            entries = emptyList(),
            next = upstreamResolver,
            scalarMapping = scalarMapping,
            requiresOptInAnnotation = requiresOptInAnnotation,
            hooks = compilerKotlinHooks
        ),
        targetLanguageVersion = targetLanguageVersion,
    )
    val builders = mutableListOf<CgFileBuilder>()

    if (irSchema != null) {
      builders.addAll(
          schemaFileBuilders(context,
              sealedClassesForEnumsMatching,
              generateDataBuilders,
              generateInputBuilders,
              generateSchema,
              generatedSchemaName,
              scalarMapping,
              irSchema)
      )
    }

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


    buildFileSpecs(builders, compilerKotlinHooks).forEach {
      it.writeTo(outputDir)
    }

    val resolverInfo = ResolverInfo(
        magic = "KotlinCodegen",
        version = APOLLO_VERSION,
        entries = context.resolver.entries()
    )

    if (codegenMetadataFile != null) {
      CodegenMetadata(
          resolverInfo
      ).writeTo(codegenMetadataFile)
    }
  }

  fun schemaFileSpecs(
      codegenSchema: CodegenSchema,
      packageName: String,
  ): Pair<CodegenMetadata, List<FileSpec>> {
    val layout = CodegenLayout(
        codegenSchema = codegenSchema,
        packageNameGenerator = PackageNameGenerator.Flat(packageName),
        useSemanticNaming = false,
        decapitalizeFields = false,
    )

    val context = KotlinContext(
        generateMethods = emptyList(),
        jsExport = false,
        layout = layout,
        resolver = KotlinResolver(
            entries = emptyList(),
            next = null,
            scalarMapping = codegenSchema.scalarMapping,
            requiresOptInAnnotation = null,
            hooks = emptyList()
        ),
        targetLanguageVersion = TargetLanguage.KOTLIN_1_9,
    )
    val builders = mutableListOf<CgFileBuilder>()
    codegenSchema.schema
        .typeDefinitions
        .values
        .filterIsInstance<GQLEnumTypeDefinition>()
        .map {
          it.toIr(schema = codegenSchema.schema)
        }.forEach { irEnum ->
          builders.add(EnumAsEnumBuilder(context, irEnum, false))
          builders.add(EnumResponseAdapterBuilder(context, irEnum))
        }

    codegenSchema.schema
        .typeDefinitions
        .values
        .filterIsInstance<GQLInputObjectTypeDefinition>()
        .map {
          it.toIr(schema = codegenSchema.schema)
        }.forEach { irInputObject ->
          builders.add(InputObjectBuilder(context, irInputObject, true, false))
          builders.add(InputObjectAdapterBuilder(context, irInputObject))
        }

    val fileSpecs = buildFileSpecs(builders, compilerKotlinHooks(null, true))
    return CodegenMetadata(ResolverInfo(
        magic = "KotlinCodegen",
        version = APOLLO_VERSION,
        entries = context.resolver.entries()
    )) to fileSpecs
  }

  fun resolverFileSpecs(
      codegenSchema: CodegenSchema,
      codegenMetadata: CodegenMetadata,
      irTargetObjects: List<IrTargetObject>,
      packageName: String,
      serviceName: String,
  ): List<FileSpec> {
    val layout = CodegenLayout(
        codegenSchema = codegenSchema,
        packageNameGenerator = PackageNameGenerator.Flat(packageName),
        useSemanticNaming = false,
        decapitalizeFields = false,
    )

    val upstreamResolver = KotlinResolver(
        entries = codegenMetadata.resolverInfo.entries,
        next = null,
        scalarMapping = codegenSchema.scalarMapping,
        requiresOptInAnnotation = null,
        hooks = emptyList()
    )
    val context = KotlinContext(
        generateMethods = emptyList(),
        jsExport = false,
        layout = layout,
        resolver = KotlinResolver(
            entries = emptyList(),
            next = upstreamResolver,
            scalarMapping = codegenSchema.scalarMapping,
            requiresOptInAnnotation = null,
            hooks = emptyList()
        ),
        targetLanguageVersion = TargetLanguage.KOTLIN_1_9,
    )

    val builders = mutableListOf<CgFileBuilder>()

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

    return buildFileSpecs(builders, compilerKotlinHooks(null, true))
  }
}

