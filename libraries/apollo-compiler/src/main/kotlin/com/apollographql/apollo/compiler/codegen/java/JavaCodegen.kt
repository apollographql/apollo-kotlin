package com.apollographql.apollo.compiler.codegen.java

import com.apollographql.apollo.compiler.APOLLO_VERSION
import com.apollographql.apollo.compiler.CodegenMetadata
import com.apollographql.apollo.compiler.CodegenSchema
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
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentDataAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentModelsBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentSelectionsBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.FragmentVariablesAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationSelectionsBuilder
import com.apollographql.apollo.compiler.codegen.java.operations.OperationVariablesAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.BuilderFactoryBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.EnumAsClassBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.EnumAsEnumBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.EnumResponseAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InputObjectAdapterBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InputObjectBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InterfaceBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InterfaceBuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InterfaceMapBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.InterfaceUnknownMapBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.ObjectBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.ObjectBuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.ObjectMapBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.ScalarBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.SchemaBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UnionBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UnionBuilderBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UnionMapBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UnionUnknownMapBuilder
import com.apollographql.apollo.compiler.codegen.java.schema.UtilAssertionsBuilder
import com.apollographql.apollo.compiler.defaultClassesForEnumsMatching
import com.apollographql.apollo.compiler.defaultGenerateFragmentImplementations
import com.apollographql.apollo.compiler.defaultGenerateModelBuilders
import com.apollographql.apollo.compiler.defaultGeneratePrimitiveTypes
import com.apollographql.apollo.compiler.defaultGenerateQueryDocument
import com.apollographql.apollo.compiler.defaultGenerateSchema
import com.apollographql.apollo.compiler.defaultNullableFieldStyle
import com.apollographql.apollo.compiler.generateMethodsJava
import com.apollographql.apollo.compiler.ir.DefaultIrSchema
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
    codegenSchema: CodegenSchema,
    upstreamCodegenMetadata: List<CodegenMetadata>,
    generatePrimitiveTypes: Boolean,
    nullableFieldStyle: JavaNullable,
    javaOutputTransform: Transform<JavaOutput>?,
    block: OutputBuilder.(resolver: JavaResolver) -> Unit,
): JavaOutput {
  val resolver = JavaResolver(
      entries = upstreamCodegenMetadata.flatMap { it.entries },
      next = null,
      scalarMapping = codegenSchema.scalarMapping,
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
      CodegenMetadata(
          targetLanguage = TargetLanguage.JAVA,
          entries = resolver.entries()
      )
  ).maybeTransform(javaOutputTransform)
}

internal object JavaCodegen {
  fun buildSchemaSources(
      codegenSchema: CodegenSchema,
      irSchema: IrSchema,
      codegenOptions: JavaSchemaCodegenOptions,
      layout: SchemaLayout,
      javaOutputTransform: Transform<JavaOutput>?,
  ): JavaOutput {
    check(irSchema is DefaultIrSchema)

    val generateDataBuilders = codegenSchema.generateDataBuilders

    val generateMethods = generateMethodsJava(codegenOptions.generateMethods)
    val generateSchema = codegenOptions.generateSchema ?: defaultGenerateSchema || generateDataBuilders

    val classesForEnumsMatching = codegenOptions.classesForEnumsMatching ?: defaultClassesForEnumsMatching
    val generateModelBuilders = codegenOptions.generateModelBuilders ?: defaultGenerateModelBuilders
    val generatePrimitiveTypes = codegenOptions.generatePrimitiveTypes ?: defaultGeneratePrimitiveTypes
    val nullableFieldStyle = codegenOptions.nullableFieldStyle ?: defaultNullableFieldStyle

    val scalarMapping = codegenSchema.scalarMapping

    return buildOutput(
        codegenSchema = codegenSchema,
        upstreamCodegenMetadata = emptyList(),
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
        builders.add(ScalarBuilder(context, irScalar, scalarMapping.get(irScalar.name)?.targetName))
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
        builders.add(SchemaBuilder(context, scalarMapping, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions, irSchema.irEnums))
      }
      if (generateDataBuilders) {
        builders.add(BuilderFactoryBuilder(context, irSchema.irObjects, irSchema.irInterfaces, irSchema.irUnions))
      }
    }
  }

  fun buildOperationsSources(
      codegenSchema: CodegenSchema,
      irOperations: IrOperations,
      operationOutput: OperationOutput,
      upstreamCodegenMetadata: List<CodegenMetadata>,
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
    val generateDataBuilders = codegenSchema.generateDataBuilders

    val generateFragmentImplementations = codegenOptions.generateFragmentImplementations ?: defaultGenerateFragmentImplementations
    val generateMethods = generateMethodsJava(codegenOptions.generateMethods)
    val generateQueryDocument = codegenOptions.generateQueryDocument ?: defaultGenerateQueryDocument

    val generateModelBuilders = codegenOptions.generateModelBuilders ?: defaultGenerateModelBuilders
    val generatePrimitiveTypes = codegenOptions.generatePrimitiveTypes ?: defaultGeneratePrimitiveTypes
    val nullableFieldStyle = codegenOptions.nullableFieldStyle ?: defaultNullableFieldStyle

    return buildOutput(
        codegenSchema = codegenSchema,
        upstreamCodegenMetadata = upstreamCodegenMetadata,
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
                    generateDataBuilders = generateDataBuilders
                )
            )
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
