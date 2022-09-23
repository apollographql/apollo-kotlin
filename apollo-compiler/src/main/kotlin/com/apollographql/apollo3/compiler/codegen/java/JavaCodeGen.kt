package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.JavaNullable
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.ScalarInfo
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
import com.apollographql.apollo3.compiler.codegen.java.file.InterfaceMapBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectBuilderBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectMapBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionMapBuilder
import com.apollographql.apollo3.compiler.ir.Ir
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import java.io.File


internal class JavaCodeGen(
    private val ir: Ir,
    private val resolverInfos: List<ResolverInfo>,
    private val useSemanticNaming: Boolean,
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    private val useSchemaPackageNameForFragments: Boolean,
    /**
     * The operation id cannot be set in [IrOperation] because it needs access to [IrOperation.sourceWithFragments]
     * So we do this in the codegen step
     */
    private val operationOutput: OperationOutput,
    private val generateFragmentImplementations: Boolean,
    private val generateQueryDocument: Boolean,
    private val generateSchema: Boolean,
    private val generatedSchemaName: String,
    private val generateModelBuilder: Boolean,
    /**
     * Whether to flatten the models. This decision is left to the codegen. For fragments for an example, we
     * want to flatten at depth 1 to avoid name clashes, but it's ok to flatten fragment response adapters at
     * depth 0
     */
    private val flatten: Boolean,
    private val classesForEnumsMatching: List<String>,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val generateDataBuilders: Boolean,
    private val generatePrimitiveTypes: Boolean,
    private val nullableFieldStyle: JavaNullable,
) {
  /**
   * @param outputDir: the directory where to write the Kotlin files
   * @return a ResolverInfo to be used by downstream modules
   */
  fun write(outputDir: File): ResolverInfo {
    val upstreamResolver = resolverInfos.fold(null as JavaResolver?) { acc, resolverInfo ->
      JavaResolver(resolverInfo.entries, acc, scalarMapping, generatePrimitiveTypes, nullableFieldStyle)
    }

    val layout = JavaCodegenLayout(
        ir = ir,
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        schemaPackageName = schemaPackageName,
        useSchemaPackageNameForFragments = useSchemaPackageNameForFragments,
    )

    val context = JavaContext(
        layout = layout,
        resolver = JavaResolver(emptyList(), upstreamResolver, scalarMapping, generatePrimitiveTypes, nullableFieldStyle),
        generateModelBuilder = generateModelBuilder,
        nullableFieldStyle = nullableFieldStyle,
    )
    val builders = mutableListOf<JavaClassBuilder>()

    ir.inputObjects
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach {
          builders.add(InputObjectBuilder(context, it))
          builders.add(InputObjectAdapterBuilder(context, it))
        }
    ir.enums
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { enum ->
          if (classesForEnumsMatching.any { Regex(it).matches(enum.name) }) {
            builders.add(EnumAsClassBuilder(context, enum))
          } else {
            builders.add(EnumAsEnumBuilder(context, enum))
          }
          builders.add(EnumResponseAdapterBuilder(context, enum))
        }
    ir.objects
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { obj ->
          builders.add(ObjectBuilder(context, obj))
          if (generateDataBuilders) {
            builders.add(ObjectBuilderBuilder(context, obj))
            builders.add(ObjectMapBuilder(context, obj))
          }
        }
    ir.interfaces
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { iface ->
          builders.add(InterfaceBuilder(context, iface))
          if (generateDataBuilders) {
            builders.add(InterfaceMapBuilder(context, iface))
          }
        }
    ir.unions
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { union ->
          builders.add(UnionBuilder(context, union))
          if (generateDataBuilders) {
            builders.add(UnionMapBuilder(context, union))
          }
        }
    ir.customScalars
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { customScalar ->
          builders.add(CustomScalarBuilder(context, customScalar))
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

    if (generateSchema && context.resolver.resolve(ResolverKey(ResolverKeyKind.Schema, "")) == null) {
      builders.add(SchemaBuilder(context, generatedSchemaName, scalarMapping, ir.objects, ir.interfaces, ir.unions, ir.enums))
    }
    if (generateDataBuilders) {
      builders.add(BuilderFactoryBuilder(context, ir.objects))
    }

    builders.forEach { it.prepare() }
    builders
        .map {
          it.build()
        }.forEach {
          val builder = JavaFile.builder(
              it.packageName,
              it.typeSpec
          ).addFileComment(
              """
                
                AUTO-GENERATED FILE. DO NOT MODIFY.
                
                This class was automatically generated by Apollo GraphQL version '$APOLLO_VERSION'.
                
              """.trimIndent()
          )

          builder
              .build()
              .writeTo(outputDir)
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
