package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.CodegenSchema
import com.apollographql.apollo3.compiler.CommonCodegenOptions
import com.apollographql.apollo3.compiler.KotlinCodegenOptions
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.allTypes
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
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.ir.IrSchema
import com.apollographql.apollo3.compiler.ir.IrTargetObject
import com.apollographql.apollo3.compiler.ir.toIr
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

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
      generateAsInternal: Boolean,
      hooks: ApolloCompilerKotlinHooks,
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

          cgFile.typeSpecs.map { typeSpec -> typeSpec.internal(generateAsInternal) }.forEach { typeSpec ->
            builder.addType(typeSpec)
          }
          cgFile.funSpecs.map { funSpec -> funSpec.internal(generateAsInternal) }.forEach { funSpec ->
            builder.addFunction(funSpec)
          }
          cgFile.propertySpecs.map { propertySpec -> propertySpec.internal(generateAsInternal) }.forEach { propertySpec ->
            builder.addProperty(propertySpec)
          }
          cgFile.imports.forEach {
            builder.addAliasedImport(it.className, it.alias)
          }
          ApolloCompilerKotlinHooks.FileInfo(fileSpec = builder.build())
        }
        .let { hooks.postProcessFiles(it) }

    // Write the files to disk
    return fileInfos.map { it.fileSpec }
  }

  /**
   * @return a ResolverInfo to be used by downstream modules
   */
  fun writeOperations(
      commonCodegenOptions: CommonCodegenOptions,
      kotlinCodegenOptions: KotlinCodegenOptions,
  ): ResolverInfo {
    val ir = commonCodegenOptions.ir
    check(ir is DefaultIrOperations)

    val operationOutput = commonCodegenOptions.operationOutput
    val resolverInfos = commonCodegenOptions.incomingCodegenMetadata.map { it.resolverInfo }
    val generateAsInternal = kotlinCodegenOptions.generateAsInternal
    val generateMethods = commonCodegenOptions.generateMethods
    val generateFilterNotNull = kotlinCodegenOptions.generateFilterNotNull
    val generateFragmentImplementations = commonCodegenOptions.generateFragmentImplementations
    val generateQueryDocument = commonCodegenOptions.generateQueryDocument
    val generatedSchemaName = commonCodegenOptions.generatedSchemaName
    val generateDataBuilders = ir.generateDataBuilders
    val flatten = ir.flattenModels
    val sealedClassesForEnumsMatching = kotlinCodegenOptions.sealedClassesForEnumsMatching
    val targetLanguageVersion = kotlinCodegenOptions.languageVersion
    val scalarMapping = commonCodegenOptions.codegenSchema.scalarMapping
    val addJvmOverloads = kotlinCodegenOptions.addJvmOverloads
    val jsExport = kotlinCodegenOptions.jsExport
    val requiresOptInAnnotation = kotlinCodegenOptions.requiresOptInAnnotation
    val decapitalizeFields = ir.decapitalizeFields
    val hooks = kotlinCodegenOptions.compilerKotlinHooks
    val generateSchema = commonCodegenOptions.generateSchema || generateDataBuilders
    val outputDir = commonCodegenOptions.outputDir
    val generateInputBuilders = kotlinCodegenOptions.generateInputBuilders

    val upstreamResolver = resolverInfos.fold(null as KotlinResolver?) { acc, resolverInfo ->
      KotlinResolver(resolverInfo.entries, acc, scalarMapping, requiresOptInAnnotation, hooks)
    }

    val irSchema = commonCodegenOptions.irSchema

    val layout = KotlinCodegenLayout(
        allTypes = commonCodegenOptions.codegenSchema.allTypes(),
        useSemanticNaming = commonCodegenOptions.useSemanticNaming,
        packageNameGenerator = commonCodegenOptions.packageNameGenerator,
        schemaPackageName = commonCodegenOptions.codegenSchema.packageName,
        decapitalizeFields = decapitalizeFields,
    )

    val context = KotlinContext(
        generateMethods = generateMethods,
        jsExport = jsExport,
        layout = layout,
        resolver = KotlinResolver(emptyList(), upstreamResolver, scalarMapping, requiresOptInAnnotation, hooks),
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
              commonCodegenOptions.codegenSchema.scalarMapping,
              irSchema)
      )
    }

    ir.fragments
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

    ir.operations
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

    buildFileSpecs(builders, generateAsInternal, hooks).forEach {
      it.writeTo(outputDir)
    }

    return ResolverInfo(
        magic = "KotlinCodegen",
        version = APOLLO_VERSION,
        entries = context.resolver.entries()
    )
  }

  private fun TypeSpec.internal(generateAsInternal: Boolean): TypeSpec {
    return if (generateAsInternal) {
      this.toBuilder().addModifiers(KModifier.INTERNAL).build()
    } else {
      this
    }
  }

  private fun FunSpec.internal(generateAsInternal: Boolean): FunSpec {
    return if (generateAsInternal) {
      this.toBuilder().addModifiers(KModifier.INTERNAL).build()
    } else {
      this
    }
  }

  private fun PropertySpec.internal(generateAsInternal: Boolean): PropertySpec {
    return if (generateAsInternal) {
      this.toBuilder().addModifiers(KModifier.INTERNAL).build()
    } else {
      this
    }
  }

  fun schemaFileSpecs(
      codegenSchema: CodegenSchema,
      packageName: String,
  ): Pair<CodegenMetadata, List<FileSpec>> {
    val layout = KotlinCodegenLayout(
        allTypes = codegenSchema.allTypes(),
        useSemanticNaming = false,
        packageNameGenerator = PackageNameGenerator.Flat(packageName),
        schemaPackageName = codegenSchema.packageName,
        decapitalizeFields = false,
    )

    val context = KotlinContext(
        generateMethods = emptyList(),
        jsExport = false,
        layout = layout,
        resolver = KotlinResolver(emptyList(), null, codegenSchema.scalarMapping, null, ApolloCompilerKotlinHooks.Identity),
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

    val fileSpecs = buildFileSpecs(builders, true, ApolloCompilerKotlinHooks.Identity)
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
    val layout = KotlinCodegenLayout(
        allTypes = codegenSchema.allTypes(),
        useSemanticNaming = false,
        packageNameGenerator = PackageNameGenerator.Flat(packageName),
        schemaPackageName = codegenSchema.packageName,
        decapitalizeFields = false,
    )

    val upstreamResolver = KotlinResolver(codegenMetadata.resolverInfo.entries, null, codegenSchema.scalarMapping, null, ApolloCompilerKotlinHooks.Identity)
    val context = KotlinContext(
        generateMethods = emptyList(),
        jsExport = false,
        layout = layout,
        resolver = KotlinResolver(emptyList(), upstreamResolver, codegenSchema.scalarMapping, null, ApolloCompilerKotlinHooks.Identity),
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

    return buildFileSpecs(builders, true, ApolloCompilerKotlinHooks.Identity)
  }
}

