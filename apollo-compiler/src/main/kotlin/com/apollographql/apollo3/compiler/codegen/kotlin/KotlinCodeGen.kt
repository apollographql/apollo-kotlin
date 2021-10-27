package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.VersionNumber
import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.kotlin.file.EnumResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.CustomScalarBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.EnumBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.EnumCompatBuilder
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
import com.apollographql.apollo3.compiler.codegen.kotlin.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TestBuildersBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.UnionBuilder
import com.apollographql.apollo3.compiler.ir.Ir
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import java.io.File


class KotlinCodeGen(
    private val ir: Ir,
    private val resolverInfos: List<ResolverInfo>,
    private val generateAsInternal: Boolean = false,
    private val useSemanticNaming: Boolean,
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    /**
     * The operation id cannot be set in [IrOperation] because it needs access to [IrOperation.sourceWithFragments]
     * So we do this in the codegen step
     */
    private val operationOutput: OperationOutput,
    private val generateFilterNotNull: Boolean,
    private val generateFragmentImplementations: Boolean,
    private val generateQueryDocument: Boolean,
    private val generateSchema: Boolean,
    private val generateTestBuilders: Boolean,
    /**
     * Whether to flatten the models. This decision is left to the codegen. For fragments for an example, we
     * want to flatten at depth 1 to avoid name clashes, but it's ok to flatten fragment response adapters at
     * depth 0
     */
    private val flatten: Boolean,
    @Deprecated("Used for backward compatibility with 2.x")
    private val flattenNamesInOrder: Boolean,
    @Deprecated("Used for backward compatibility with 2.x")
    private val sealedClassesForEnumsMatching: List<String>,
    private val targetLanguageVersion: VersionNumber,
) {
  /**
   * @param outputDir: the directory where to write the Kotlin files
   * @return a ResolverInfo to be used by downstream modules
   */
  fun write(outputDir: File, testDir: File): ResolverInfo {
    val upstreamResolver = resolverInfos.fold(null as KotlinResolver?) { acc, resolverInfo ->
      KotlinResolver(resolverInfo.entries, acc)
    }

    val layout = CodegenLayout(
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        schemaPackageName = schemaPackageName
    )

    val context = KotlinContext(
        layout = layout,
        resolver = KotlinResolver(emptyList(), upstreamResolver),
        targetLanguageVersion = targetLanguageVersion,
    )
    val builders = mutableListOf<CgFileBuilder>()

    ir.inputObjects
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach {
          builders.add(InputObjectBuilder(context, it))
          builders.add(InputObjectAdapterBuilder(context, it))
        }
    ir.enums
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { enum ->
          if (sealedClassesForEnumsMatching.any { Regex(it).matches(enum.name) }) {
            builders.add(EnumBuilder(context, enum))
          } else {
            builders.add(EnumCompatBuilder(context, enum))
          }
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
                  flattenNamesInOrder
              )
          )

          builders.add(FragmentSelectionsBuilder(context, fragment, ir.schema, ir.allFragmentDefinitions))

          if (generateFragmentImplementations || fragment.interfaceModelGroup == null) {
            builders.add(FragmentResponseAdapterBuilder(context, fragment, flatten, flattenNamesInOrder))
          }

          if (generateFragmentImplementations) {
            builders.add(
                FragmentBuilder(
                    context,
                    generateFilterNotNull,
                    fragment,
                    flatten,
                    flattenNamesInOrder
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
          builders.add(OperationResponseAdapterBuilder(context, operation, flatten, flattenNamesInOrder))

          builders.add(
              OperationBuilder(
                  context,
                  generateFilterNotNull,
                  operationOutput.findOperationId(operation.name),
                  generateQueryDocument,
                  operation,
                  flatten,
                  flattenNamesInOrder
              )
          )

          if (generateTestBuilders) {
            builders.add(
                TestBuildersBuilder(
                    context,
                    operation.responseBasedDataModelGroup ?: error("generateTestBuilders requires generateTestBuilders"),
                    operation,
                    flatten
                )
            )
          }
        }

    if (generateSchema) {
      builders.add(SchemaBuilder(context, ir.objects, ir.interfaces, ir.unions))
    }

    builders.forEach { it.prepare() }
    builders
        .forEach { cgFileBuilder ->
          val cgFile = cgFileBuilder.build()

          val builder = FileSpec.builder(
              packageName = cgFile.packageName,
              fileName = cgFile.fileName
          ).addComment(
              """
                
                AUTO-GENERATED FILE. DO NOT MODIFY.
                
                This class was automatically generated by Apollo GraphQL version '$APOLLO_VERSION'.
                
              """.trimIndent()
          )

          cgFile.typeSpecs.map { typeSpec -> typeSpec.internal(generateAsInternal) }.forEach { typeSpec ->
            builder.addType(typeSpec)
          }

          val dir = when(cgFileBuilder) {
            is CgOutputFileBuilder -> outputDir
            is CgTestFileBuilder ->testDir
          }
          builder
              .build()
              .writeTo(dir)
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
}
