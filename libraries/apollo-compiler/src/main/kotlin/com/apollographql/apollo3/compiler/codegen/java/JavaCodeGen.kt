package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.CommonCodegenOptions
import com.apollographql.apollo3.compiler.JavaCodegenOptions
import com.apollographql.apollo3.compiler.allTypes
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.java.adapter.EnumResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.BuilderFactoryBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.CustomScalarBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.EnumAsClassBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.EnumAsEnumBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentDataAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentModelsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InputObjectAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InputObjectBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InterfaceBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InterfaceBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InterfaceMapBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InterfaceUnknownMapBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectMapBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionMapBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionUnknownMapBuilder
import com.apollographql.apollo3.compiler.hooks.ApolloCompilerJavaHooks
import com.apollographql.apollo3.compiler.ir.DefaultIrOperations
import com.apollographql.apollo3.compiler.ir.DefaultIrSchema
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import java.io.File


internal class JavaCodeGen(
    private val commonCodegenOptions: CommonCodegenOptions,
    private val javaCodegenOptions: JavaCodegenOptions,
) {
  /**
   * @param outputDir: the directory where to write the Kotlin files
   * @return a ResolverInfo to be used by downstream modules
   */
  fun write(outputDir: File): ResolverInfo {
    val ir = commonCodegenOptions.ir
    check(ir is DefaultIrOperations)

    val resolverInfos = commonCodegenOptions.incomingCodegenMetadata.map { it.resolverInfo }
    val operationOutput = commonCodegenOptions.operationOutput
    val useSemanticNaming = commonCodegenOptions.useSemanticNaming
    val packageNameGenerator = commonCodegenOptions.packageNameGenerator
    val schemaPackageName = commonCodegenOptions.codegenSchema.packageName
    val generateFragmentImplementations = commonCodegenOptions.generateFragmentImplementations
    val generateQueryDocument = commonCodegenOptions.generateQueryDocument
    val generatedSchemaName = commonCodegenOptions.generatedSchemaName
    val flatten = ir.flattenModels
    val classesForEnumsMatching = javaCodegenOptions.classesForEnumsMatching
    val scalarMapping = commonCodegenOptions.codegenSchema.scalarMapping
    val generateDataBuilders = ir.generateDataBuilders
    val generateModelBuilders = javaCodegenOptions.generateModelBuilders
    val generatePrimitiveTypes = javaCodegenOptions.generatePrimitiveTypes
    val nullableFieldStyle = javaCodegenOptions.nullableFieldStyle
    val decapitalizeFields = ir.decapitalizeFields
    val hooks = javaCodegenOptions.compilerJavaHooks
    val generateSchema = commonCodegenOptions.generateSchema || generateDataBuilders

    val upstreamResolver = resolverInfos.fold(null as JavaResolver?) { acc, resolverInfo ->
      JavaResolver(resolverInfo.entries, acc, scalarMapping, generatePrimitiveTypes, nullableFieldStyle, hooks)
    }
    val irSchema = commonCodegenOptions.irSchema

    val layout = JavaCodegenLayout(
        allTypes = commonCodegenOptions.codegenSchema.allTypes(),
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        schemaPackageName = schemaPackageName,
        decapitalizeFields = decapitalizeFields,
    )

    val context = JavaContext(
        layout = layout,
        resolver = JavaResolver(emptyList(), upstreamResolver, scalarMapping, generatePrimitiveTypes, nullableFieldStyle, hooks),
        generateModelBuilders = generateModelBuilders,
        nullableFieldStyle = nullableFieldStyle,
    )
    val builders = mutableListOf<JavaClassBuilder>()


    if (irSchema is DefaultIrSchema) {
      irSchema.irScalars.forEach { irScalar ->
        builders.add(CustomScalarBuilder(context, irScalar, scalarMapping.get(irScalar.name)?.targetName))
      }
      irSchema.irEnums.forEach { irEnum ->
        if (classesForEnumsMatching.any { Regex(it).matches(irEnum.name) }) {
          builders.add(EnumAsClassBuilder(context, irEnum))
        } else {
          builders.add(EnumAsEnumBuilder(context, irEnum))
        }
        builders.add(EnumResponseAdapterBuilder(context, irEnum))
      }
      irSchema.irInputObjects.forEach { irInputObject ->
        builders.add(InputObjectBuilder(context, irInputObject))
        builders.add(InputObjectAdapterBuilder(context, irInputObject))
      }
      irSchema.irUnions.forEach { irUnion ->
        builders.add(UnionBuilder(context, irUnion))
        if (generateDataBuilders) {
          builders.add(UnionBuilderBuilder(context, irUnion))
          builders.add(UnionUnknownMapBuilder(context, irUnion))
          builders.add(UnionMapBuilder(context, irUnion))
        }
      }
      irSchema.irInterfaces.forEach { irInterface ->
        builders.add(InterfaceBuilder(context, irInterface))
        if (generateDataBuilders) {
          builders.add(InterfaceBuilderBuilder(context, irInterface))
          builders.add(InterfaceUnknownMapBuilder(context, irInterface))
          builders.add(InterfaceMapBuilder(context, irInterface))
        }
      }
      irSchema.irObjects.forEach { irObject ->
        builders.add(ObjectBuilder(context, irObject))
        if (generateDataBuilders) {
          builders.add(ObjectBuilderBuilder(context, irObject))
          builders.add(ObjectMapBuilder(context, irObject))
        }
      }
      if (generateSchema && context.resolver.resolve(ResolverKey(ResolverKeyKind.Schema, "")) == null) {
        builders.add(SchemaBuilder(context, generatedSchemaName, scalarMapping, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions, irSchema.irEnums))
      }
      if (generateDataBuilders) {
        builders.add(BuilderFactoryBuilder(context, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions))
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
            builders.add(FragmentDataAdapterBuilder(context, fragment, flatten))
          }

          if (generateFragmentImplementations) {
            builders.add(
                FragmentBuilder(
                    context,
                    fragment,
                    flatten,
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
                  context = context,
                  operationId = operationOutput.findOperationId(operation.name),
                  generateQueryDocument = generateQueryDocument,
                  operation = operation,
                  flatten = flatten,
                  generateDataBuilders = generateDataBuilders
              )
          )
        }

    builders.forEach { it.prepare() }
    val fileInfos = builders
        .map {
          val codegenJavaFile = it.build()
          val javaFile = JavaFile.builder(
              codegenJavaFile.packageName,
              codegenJavaFile.typeSpec
          ).addFileComment(
              """
                
                AUTO-GENERATED FILE. DO NOT MODIFY.
                
                This class was automatically generated by Apollo GraphQL version '$APOLLO_VERSION'.
                
              """.trimIndent()
          )
              .build()
          ApolloCompilerJavaHooks.FileInfo(javaFile = javaFile)
        }
        .let { hooks.postProcessFiles(it) }

    // Write the files to disk
    fileInfos.forEach {
      it.javaFile.writeTo(outputDir)
    }

    return ResolverInfo(
        magic = "KotlinCodegen",
        version = APOLLO_VERSION,
        entries = context.resolver.entries()
    )
  }
}

fun List<CodeBlock>.joinToCode(separator: String, prefix: String = "", suffix: String = ""): CodeBlock {
  var first = true
  return fold(
      CodeBlock.builder().add(prefix)
  ) { builder, block ->
    if (first) {
      first = false
    } else {
      builder.add(separator)
    }
    builder.add(L, block)
  }.add(suffix)
      .build()
}

fun CodeBlock.isNotEmpty() = isEmpty().not()

internal const val T = "${'$'}T"
internal const val L = "${'$'}L"
internal const val S = "${'$'}S"
