package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.CommonCodegenOptions
import com.apollographql.apollo3.compiler.KotlinCodegenOptions
import com.apollographql.apollo3.compiler.allTypes
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.kotlin.file.CustomScalarAdaptersBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.ScalarBuilder
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
import com.apollographql.apollo3.compiler.codegen.kotlin.file.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.PaginationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.UnionBuilder
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerKotlinHooks
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal object KotlinCodeGen {
  /**
   * @return a ResolverInfo to be used by downstream modules
   */
  fun write(
      commonCodegenOptions: CommonCodegenOptions,
      kotlinCodegenOptions: KotlinCodegenOptions,
  ): ResolverInfo {
    val ir = commonCodegenOptions.ir
    check(ir is DefaultIrOperations)

    val operationOutput = commonCodegenOptions.operationOutput
    val resolverInfos = commonCodegenOptions.incomingCodegenMetadata.map { it.resolverInfo }
    val generateAsInternal = kotlinCodegenOptions.generateAsInternal
    val useSemanticNaming = commonCodegenOptions.useSemanticNaming
    val packageNameGenerator = commonCodegenOptions.packageNameGenerator
    val schemaPackageName = commonCodegenOptions.codegenSchema.packageName
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
    val requiresOptInAnnotation = kotlinCodegenOptions.requiresOptInAnnotation
    val decapitalizeFields = ir.decapitalizeFields
    val hooks = kotlinCodegenOptions.compilerKotlinHooks
    val generateSchema = commonCodegenOptions.generateSchema || generateDataBuilders
    val outputDir = commonCodegenOptions.outputDir

    val upstreamResolver = resolverInfos.fold(null as KotlinResolver?) { acc, resolverInfo ->
      KotlinResolver(resolverInfo.entries, acc, scalarMapping, requiresOptInAnnotation, hooks)
    }

    val irSchema = commonCodegenOptions.irSchema

    val layout = KotlinCodegenLayout(
        allTypes = commonCodegenOptions.codegenSchema.allTypes(),
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        schemaPackageName = schemaPackageName,
        decapitalizeFields = decapitalizeFields,
    )

    val context = KotlinContext(
        layout = layout,
        resolver = KotlinResolver(emptyList(), upstreamResolver, scalarMapping, requiresOptInAnnotation, hooks),
        targetLanguageVersion = targetLanguageVersion,
    )
    val builders = mutableListOf<CgFileBuilder>()

    if (irSchema is DefaultIrSchema) {
      irSchema.irScalars.forEach {irScalar ->
        builders.add(ScalarBuilder(context, irScalar, scalarMapping.get(irScalar.name)?.targetName))
      }
      irSchema.irEnums.forEach {irEnum ->
        if (sealedClassesForEnumsMatching.any { Regex(it).matches(irEnum.name) }) {
          builders.add(EnumAsSealedBuilder(context, irEnum))
        } else {
          builders.add(EnumAsEnumBuilder(context, irEnum))
        }
        builders.add(EnumResponseAdapterBuilder(context, irEnum))
      }
      irSchema.irInputObjects.forEach { irInputObject ->
        builders.add(InputObjectBuilder(context, irInputObject))
        builders.add(InputObjectAdapterBuilder(context, irInputObject))
      }
      irSchema.irUnions.forEach {irUnion ->
        builders.add(UnionBuilder(context, irUnion, generateDataBuilders))
      }
      irSchema.irInterfaces.forEach {irInterface ->
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
                    generateDataBuilders
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
                  generateDataBuilders
              )
          )
        }


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
          ApolloCompilerKotlinHooks.FileInfo(fileSpec = builder.build())
        }
        .let { hooks.postProcessFiles(it) }

    // Write the files to disk
    fileInfos.forEach {
      it.fileSpec.writeTo(outputDir)
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
}
