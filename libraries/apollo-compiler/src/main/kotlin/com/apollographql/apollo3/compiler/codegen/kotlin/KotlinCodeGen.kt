package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.APOLLO_VERSION
import com.apollographql.apollo3.compiler.PackageNameGenerator
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.TargetLanguage
import com.apollographql.apollo3.compiler.codegen.ResolverInfo
import com.apollographql.apollo3.compiler.codegen.ResolverKey
import com.apollographql.apollo3.compiler.codegen.ResolverKeyKind
import com.apollographql.apollo3.compiler.codegen.kotlin.file.CustomScalarBuilder
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
import com.apollographql.apollo3.compiler.codegen.kotlin.file.ObjectBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationResponseAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationSelectionsBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.OperationVariablesAdapterBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.PaginationBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.SchemaBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.TestBuildersBuilder
import com.apollographql.apollo3.compiler.codegen.kotlin.file.UnionBuilder
import com.apollographql.apollo3.compiler.ir.Ir
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File


internal class KotlinCodeGen(
    private val ir: Ir,
    private val resolverInfos: List<ResolverInfo>,
    private val generateAsInternal: Boolean = false,
    private val useSemanticNaming: Boolean,
    private val packageNameGenerator: PackageNameGenerator,
    private val schemaPackageName: String,
    private val useSchemaPackageNameForFragments: Boolean,
    /**
     * The operation id cannot be set in [IrOperation] because it needs access to [IrOperation.sourceWithFragments]
     * So we do this in the codegen step
     */
    private val operationOutput: OperationOutput,
    private val generateFilterNotNull: Boolean,
    private val generateFragmentImplementations: Boolean,
    private val generateQueryDocument: Boolean,
    private val generateSchema: Boolean,
    private val generatedSchemaName: String,
    private val generateTestBuilders: Boolean,
    private val generateDataBuilders: Boolean,
    /**
     * Whether to flatten the models. This decision is left to the codegen. For fragments for an example, we
     * want to flatten at depth 1 to avoid name clashes, but it's ok to flatten fragment response adapters at
     * depth 0
     */
    private val flatten: Boolean,
    private val sealedClassesForEnumsMatching: List<String>,
    private val targetLanguageVersion: TargetLanguage,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val addJvmOverloads: Boolean,
    private val requiresOptInAnnotation: String?,
) {
  /**
   * @param outputDir: the directory where to write the Kotlin files
   * @return a ResolverInfo to be used by downstream modules
   */
  fun write(outputDir: File, testDir: File): ResolverInfo {
    val upstreamResolver = resolverInfos.fold(null as KotlinResolver?) { acc, resolverInfo ->
      KotlinResolver(resolverInfo.entries, acc, scalarMapping, requiresOptInAnnotation)
    }

    val layout = KotlinCodegenLayout(
        ir = ir,
        useSemanticNaming = useSemanticNaming,
        packageNameGenerator = packageNameGenerator,
        schemaPackageName = schemaPackageName,
        useSchemaPackageNameForFragments = useSchemaPackageNameForFragments,
    )

    val context = KotlinContext(
        layout = layout,
        resolver = KotlinResolver(emptyList(), upstreamResolver, scalarMapping, requiresOptInAnnotation),
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
            builders.add(EnumAsSealedBuilder(context, enum))
          } else {
            builders.add(EnumAsEnumBuilder(context, enum))
          }
          builders.add(EnumResponseAdapterBuilder(context, enum))
        }
    ir.objects
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { obj ->
          builders.add(ObjectBuilder(context, obj, generateDataBuilders))
        }
    ir.interfaces
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { iface ->
          builders.add(InterfaceBuilder(context, iface, generateDataBuilders))
        }
    ir.unions
        .filter { !context.resolver.canResolveSchemaType(it.name) }
        .forEach { union ->
          builders.add(UnionBuilder(context, union, generateDataBuilders))
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

    if (generateSchema && context.resolver.resolve(ResolverKey(ResolverKeyKind.Schema, "")) == null) {
      builders.add(SchemaBuilder(context, generatedSchemaName, ir.objects, ir.interfaces, ir.unions, ir.enums))
      builders.add(CustomScalarAdaptersBuilder(context, scalarMapping))
    }

    if (ir.connectionTypes.isNotEmpty() && context.resolver.resolve(ResolverKey(ResolverKeyKind.Pagination, "")) == null ) {
      builders.add(PaginationBuilder(context, ir.connectionTypes))
    }

    /**
     * 1st pass: call prepare on all builders
     */
    builders.forEach { it.prepare() }

    /**
     * 2nd pass: build the [CgFile]s
     */
    builders.map { it.build() }
        .forEach { cgFile ->
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
          val dir = if (cgFile.isTest) {
            testDir
          } else {
            outputDir
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
