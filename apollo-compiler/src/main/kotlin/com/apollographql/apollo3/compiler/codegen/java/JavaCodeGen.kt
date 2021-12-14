package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.java.adapter.EnumResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.CustomScalarBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.EnumBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentDataAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentModelsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.FragmentVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InputObjectAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InputObjectBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.InterfaceBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.java.file.UnionBuilder
import com.apollographql.apollo3.compiler.ir.Ir
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import java.io.File


class JavaCodeGen(
    private val ir: Ir,
    private val resolverInfos: List<ResolverInfo>,
    private val useSemanticNaming: Boolean,
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    /**
     * The operation id cannot be set in [IrOperation] because it needs access to [IrOperation.sourceWithFragments]
     * So we do this in the codegen step
     */
    private val operationOutput: OperationOutput,
    private val generateFragmentImplementations: Boolean,
    private val generateQueryDocument: Boolean,
    private val generateSchema: Boolean,
    /**
     * Whether to flatten the models. This decision is left to the codegen. For fragments for an example, we
     * want to flatten at depth 1 to avoid name clashes, but it's ok to flatten fragment response adapters at
     * depth 0
     */
    private val flatten: Boolean,
) {
  /**
   * @param outputDir: the directory where to write the Kotlin files
   * @return a ResolverInfo to be used by downstream modules
   */
  fun write(outputDir: File): ResolverInfo {
    val upstreamResolver = resolverInfos.fold(null as JavaResolver?) { acc, resolverInfo ->
      JavaResolver(resolverInfo.entries, acc)
    }

    val layout = JavaCodegenLayout(
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        schemaPackageName = schemaPackageName
    )

    val context = JavaContext(
        layout = layout,
        resolver = JavaResolver(emptyList(), upstreamResolver)
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
          builders.add(EnumBuilder(context, enum))
          builders.add(EnumResponseAdapterBuilder(context, enum))
        }
    ir.objects
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { obj ->
          builders.add(ObjectBuilder(context, obj))
        }
    ir.interfaces
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { iface ->
          builders.add(InterfaceBuilder(context, iface))
        }
    ir.unions
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { union ->
          builders.add(UnionBuilder(context, union))
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

          builders.add(FragmentSelectionsBuilder(context, fragment, ir.schema, ir.allFragmentDefinitions))

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

          builders.add(OperationSelectionsBuilder(context, operation, ir.schema, ir.allFragmentDefinitions))
          builders.add(OperationResponseAdapterBuilder(context, operation, flatten))

          builders.add(
              OperationBuilder(
                  context,
                  operationOutput.findOperationId(operation.name),
                  generateQueryDocument,
                  operation,
                  flatten,
              )
          )
        }

    if (generateSchema) {
      builders.add(SchemaBuilder(context, ir.objects, ir.interfaces, ir.unions))
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
