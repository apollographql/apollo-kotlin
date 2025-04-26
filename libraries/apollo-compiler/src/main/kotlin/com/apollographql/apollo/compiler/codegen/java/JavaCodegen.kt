@file:Suppress("KotlinUnreachableCode")

package com.apollographql.apollo.compiler.codegen.java

import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.apollo.compiler.CODEGEN_METADATA_VERSION
import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.CodegenOptions
import com.apollographql.apollo.compiler.JavaNullable
import com.apollographql.apollo.compiler.JavaOperationsCodegenOptions
import com.apollographql.apollo.compiler.JavaSchemaCodegenOptions
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo.compiler.TargetLanguage
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.codegen.OperationsLayout
import com.apollographql.apollo.compiler.codegen.ResolverKey
import com.apollographql.apollo.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo.compiler.codegen.SchemaLayout
import com.apollographql.apollo.compiler.codegen.java.adapters.JavaOptionalAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.adapters.JavaOptionalAdaptersBuilder
import com.apollographql.apollo.compiler.codegen.java.builders.DataBuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.builders.DataInterfaceBuilder
import com.apollographql.apollo.compiler.codegen.java.builders.DataMapBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentDataAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentModelsBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentSelectionsBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentVariablesAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationSelectionsBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationVariablesAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.builders.DataBuildersBuilder
import com.apollographql.apollo.compiler.codegen.java.builders.ResolverBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.EnumAsClassBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.EnumAsEnumBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.EnumResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InputObjectAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InputObjectBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InterfaceBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.ObjectBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.ScalarBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.SchemaBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UnionBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UtilAssertionsBuilder
import com.apollographql.apollo.compiler.defaultClassesForEnumsMatching
import com.apollographql.apollo.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo.compiler.defaultGenerateModelBuilders
import com.apollographql.apollo.compiler.defaultGeneratePrimitiveTypes
import com.apollographql.apollo.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo.compiler.defaultGenerateSchema
import com.apollographql.apollo.compiler.defaultNullableFieldStyle
import com.apollographql.apollo.compiler.generateMethodsJava
import com.apollographql.apollo.compiler.ir.DefaultIrDataBuilders
import com.apollographql.apollo.compiler.ir.DefaultIrSchema
import com.apollographql.apollo.compiler.ir.IrDataBuilders
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.ir.IrSchema
import com.apollographql.apollo.compiler.maybeTransform
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.findOperationId
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile

private class OutputBuilder {
  val builders = mutableListOf<JavaClassBuilder>()
}


private fun buildOutput(
    upstreamCodegenMetadatas: List<CodegenMetadata>,
    generatePrimitiveTypes: Boolean,
    nullableFieldStyle: JavaNullable,
    javaOutputTransform: Transform<JavaOutput>?,
    block: OutputBuilder.(resolver: JavaResolver) -> Unit,
): JavaOutput {
  val upstreamCodegenMetadata = upstreamCodegenMetadatas.fold(CodegenMetadata(
      version = CODEGEN_METADATA_VERSION,
      targetLanguage = TargetLanguage.JAVA,
      emptyList(),
      emptyMap(),
      emptyMap(),
      emptyMap(),
      emptyMap()
  )) { acc, metadata ->
    acc + metadata
  }

  val resolver = JavaResolver(
      upstreamCodegenMetadata = upstreamCodegenMetadata,
      generatePrimitiveTypes = generatePrimitiveTypes,
      nullableFieldStyle = nullableFieldStyle,
  )

  val outputBuilder = OutputBuilder()
  outputBuilder.block(resolver)

  outputBuilder.builders.forEach { it.prepare() }
  val javaFiles = outputBuilder.builders
      .map {
        val codegenJavaFile = it.build()

        JavaFile.builder(
            codegenJavaFile.packageName,
            codegenJavaFile.typeSpec
        ).addFileComment(
            """
                
                AUTO-GENERATED FILE. DO NOT MODIFY.
                
                This class was automatically generated by Apollo GraphQL version '$APOLLO_VERSION'.
                
              """.trimIndent()

        ).build()
      }

  return JavaOutput(
      javaFiles,
      resolver.toCodegenMetadata(TargetLanguage.JAVA)
  ).maybeTransform(javaOutputTransform)
}

internal object JavaCodegen {
  fun buildSchemaSources(
      irSchema: IrSchema,
      codegenOptions: JavaSchemaCodegenOptions,
      layout: SchemaLayout,
      javaOutputTransform: Transform<JavaOutput>?,
  ): JavaOutput {
    check(irSchema is DefaultIrSchema)

    val generateMethods = generateMethodsJava(codegenOptions.generateMethods)
    val generateSchema = codegenOptions.generateSchema ?: defaultGenerateSchema

    val classesForEnumsMatching = codegenOptions.classesForEnumsMatching ?: defaultClassesForEnumsMatching
    val generateModelBuilders = codegenOptions.generateModelBuilders ?: defaultGenerateModelBuilders
    val generatePrimitiveTypes = codegenOptions.generatePrimitiveTypes ?: defaultGeneratePrimitiveTypes
    val nullableFieldStyle = codegenOptions.nullableFieldStyle ?: defaultNullableFieldStyle

    return buildOutput(
        upstreamCodegenMetadatas = emptyList(),
        generatePrimitiveTypes = generatePrimitiveTypes,
        nullableFieldStyle = nullableFieldStyle,
        javaOutputTransform = javaOutputTransform
    ) { resolver ->

      val context = JavaSchemaContext(
          layout = layout,
          resolver = resolver,
          generateMethods = generateMethodsJava(generateMethods),
          generateModelBuilders = generateModelBuilders,
          nullableFieldStyle = nullableFieldStyle,
      )

      irSchema.irScalars.forEach { irScalar ->
        builders.add(ScalarBuilder(context, irScalar))
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
      if (context.nullableFieldStyle == JavaNullable.GUAVA_OPTIONAL && irSchema.irInputObjects.any { it.isOneOf }) {
        // When using the Guava optionals, generate assertOneOf in the project, as apollo-api doesn't depend on Guava
        builders.add(UtilAssertionsBuilder(context))
      }
      if (context.nullableFieldStyle == JavaNullable.JAVA_OPTIONAL || context.nullableFieldStyle == JavaNullable.GUAVA_OPTIONAL) {
        builders.add(JavaOptionalAdapterBuilder(context, context.nullableFieldStyle))
        builders.add(JavaOptionalAdaptersBuilder(context))
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
        builders.add(SchemaBuilder(context, irSchema.irScalars, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions, irSchema.irEnums))
      }
    }
  }

  fun buildOperationsSources(
      irOperations: IrOperations,
      operationOutput: OperationOutput,
      upstreamCodegenMetadatas: List<CodegenMetadata>,
      codegenOptions: JavaOperationsCodegenOptions,
      layout: OperationsLayout,
      javaOutputTransform: Transform<JavaOutput>?,
  ): JavaOutput {
    if (irOperations.codegenModels != MODELS_OPERATION_BASED) {
      error("Java codegen does not support ${irOperations.codegenModels}. Only $MODELS_OPERATION_BASED is supported.")
    }
    if (!irOperations.flattenModels) {
      error("Java codegen does not support nested models as it could trigger name clashes when a nested class has the same name as an " +
          "enclosing one.")
    }

    val flatten = irOperations.flattenModels

    val generateFragmentImplementations = codegenOptions.generateFragmentImplementations ?: defaultGenerateFragmentImplementations
    val generateMethods = generateMethodsJava(codegenOptions.generateMethods)
    val generateQueryDocument = codegenOptions.generateQueryDocument ?: defaultGenerateQueryDocument

    val generateModelBuilders = codegenOptions.generateModelBuilders ?: defaultGenerateModelBuilders
    val generatePrimitiveTypes = codegenOptions.generatePrimitiveTypes ?: defaultGeneratePrimitiveTypes
    val nullableFieldStyle = codegenOptions.nullableFieldStyle ?: defaultNullableFieldStyle

    return buildOutput(
        upstreamCodegenMetadatas = upstreamCodegenMetadatas,
        generatePrimitiveTypes = generatePrimitiveTypes,
        nullableFieldStyle = nullableFieldStyle,
        javaOutputTransform = javaOutputTransform,
    ) { resolver ->
      val context = JavaOperationsContext(
          layout = layout,
          resolver = resolver,
          generateMethods = generateMethodsJava(generateMethods),
          generateModelBuilders = generateModelBuilders,
          nullableFieldStyle = nullableFieldStyle,
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

      irOperations.operations
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
                )
            )
          }
    }
  }

  fun buildDataBuilders(
      dataBuilders: IrDataBuilders,
      layout: SchemaLayout,
      codegenOptions: CodegenOptions,
      upstreamCodegenMetadata: List<CodegenMetadata>
  ): JavaOutput {
    check(dataBuilders is DefaultIrDataBuilders)

    val generatePrimitiveTypes = codegenOptions.generatePrimitiveTypes ?: defaultGeneratePrimitiveTypes
    val nullableFieldStyle = codegenOptions.nullableFieldStyle ?: defaultNullableFieldStyle
    val generateMethods = generateMethodsJava(codegenOptions.generateMethods)
    val generateModelBuilders = codegenOptions.generateModelBuilders ?: defaultGenerateModelBuilders

    return buildOutput(
        upstreamCodegenMetadatas = upstreamCodegenMetadata,
        generatePrimitiveTypes = generatePrimitiveTypes,
        nullableFieldStyle = nullableFieldStyle,
        javaOutputTransform = null
    ) { resolver ->

      val context = JavaDataBuilderContext(
          layout = layout,
          resolver = resolver,
          generateMethods = generateMethodsJava(generateMethods),
          generateModelBuilders = generateModelBuilders,
          nullableFieldStyle = nullableFieldStyle,
      )

      dataBuilders.dataBuilders.forEach {
        builders.add(DataBuilderBuilder(context, it))
        builders.add(DataMapBuilder(context, it))
        if (it.isAbstract) {
          builders.add(DataInterfaceBuilder(context, it))
        }
        builders.add(DataBuildersBuilder(context, dataBuilders.dataBuilders))
        builders.add(ResolverBuilder(context, dataBuilders.possibleTypes))
      }
    }
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

fun CodeBlock.isNotEmpty() = isEmpty.not()

internal const val T = "${'$'}T"
internal const val L = "${'$'}L"
internal const val S = "${'$'}S"
