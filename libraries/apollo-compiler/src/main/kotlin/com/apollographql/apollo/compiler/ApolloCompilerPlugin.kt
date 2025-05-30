package com.apollographql.apollo.compiler

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo.compiler.codegen.java.JavaOutput
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo.compiler.ir.IrOperations
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId
import java.io.File

/**
 * [ApolloCompilerPlugin] allows to customize the behavior of the Apollo Compiler.
 *
 * [ApolloCompilerPlugin] may be instantiated several times in a codegen run. Each instance is create in a
 * separate classloader.
 * The classloaders contains `apollo-compiler` classes and the runtime classpath of the [ApolloCompilerPlugin].
 * You may throw from [ApolloCompilerPlugin] methods to fail the build.
 */
interface ApolloCompilerPlugin {
  /**
   * This is called before each compilation step. A typical compilation involves different steps:
   * - generating the schema
   * - generating the IR
   * - generating the source code (codegen)
   *
   * @param environment options and environment for the plugin.
   * @param registry the registry where to register transformations.
   */
  fun beforeCompilationStep(environment: ApolloCompilerPluginEnvironment, registry: ApolloCompilerRegistry) {

  }

  /**
   * @return the layout or null to use the default layout
   * @param codegenSchema the codegenSchema
   */
  @Deprecated("Call `registry.registerLayout()` from beforeCompilationStep() instead.")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_2_1)
  fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
    return null
  }

  /**
   * Computes operation ids for persisted queries.
   *
   * @return a list of [OperationId] matching an operation name to its id or null to use the SHA256 default
   */
  @Deprecated("Call `registry.registerOperationIdsGenerator()` from beforeCompilationStep() instead.")
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_2_1)
  fun operationIds(descriptors: List<OperationDescriptor>): List<OperationId>? {
    return null
  }

  /**
   * @return the [Transform] to be applied to [JavaOutput] or null to use the default [Transform]
   */
  fun javaOutputTransform(): Transform<JavaOutput>? {
    return null
  }

  /**
   * @return the [Transform] to be applied to [KotlinOutput] or null to use the default [Transform]
   */
  fun kotlinOutputTransform(): Transform<KotlinOutput>? {
    return null
  }

  /**
   * @return a [DocumentTransform] to transform operations and/or fragments
   */
  @ApolloExperimental
  fun documentTransform(): DocumentTransform? {
    return null
  }

  /**
   * @return the [Transform] to be applied to [IrOperations] or null to use the default [Transform]
   */
  @ApolloExperimental
  fun irOperationsTransform(): Transform<IrOperations>? {
    return null
  }

  /**
   * @return A list of [ForeignSchema] supported by this plugin
   */
  @ApolloExperimental
  fun foreignSchemas(): List<ForeignSchema> {
    return emptyList()
  }

  /**
   * @return A [SchemaListener] called whenever the schema changed
   */
  @ApolloExperimental
  fun schemaListener(): SchemaListener? {
    return null
  }
}

@ApolloExperimental
interface SchemaListener {
  /**
   * Called when the schema changed and codegen needs to be updated
   *
   * @param schema the validated schema.
   * @param outputDirectory the compiler output directory. This directory is shared with the compiler, make sure to use a specific
   * package name to avoid clobbering other files.
   */
  fun onSchema(schema: Schema, outputDirectory: File)
}


/**
 * A [DocumentTransform] transforms operations and fragments at build time. [DocumentTransform] can add or remove fields automatically for an example.
 */
@ApolloExperimental
interface DocumentTransform {
  /**
   * Transforms the given operation.
   *
   * [transform] is called after any processing done by the Apollo compiler such as adding `__typename`.
   */
  fun transform(schema: Schema, operation: GQLOperationDefinition): GQLOperationDefinition

  /**
   * Transforms the given fragment.
   *
   * [transform] is called after any processing done by the Apollo compiler such as adding `__typename`.
   */
  fun transform(schema: Schema, fragment: GQLFragmentDefinition): GQLFragmentDefinition
}

/**
 * Transforms a type
 *
 * This is not a kotlin function type because this might be used in environment where those types are
 * relocated and might fail to load at runtime. For an example, in a Gradle plugin.
 */
interface Transform<T> {
  /**
   * Transforms the given input into an output of the same type
   */
  fun transform(input: T): T
}

/**
 * A [ExecutableDocumentTransform] transforms operations and fragments at build time. [ExecutableDocumentTransform] can add or remove fields automatically, for an example.
 */
fun interface ExecutableDocumentTransform {
  /**
   * Transforms the given document.
   *
   * [transform] is called before any validation. Implementation must be robust to invalid fragments, operations and non-executable definitions.
   * [transform] is called after any processing done by the Apollo compiler such as adding `__typename`.
   *
   * @param schema the schema
   * @param document the document containing all the operations and fragments defined in this compilation unit.
   * @param extraFragmentDefinitions extra fragment definitions from other compilation units.
   */
  fun transform(schema: Schema, document: GQLDocument, extraFragmentDefinitions: List<GQLFragmentDefinition>): GQLDocument
}

fun interface OperationIdsGenerator {
  fun generate(operationDescriptorList: Collection<OperationDescriptor>): List<OperationId>
}

sealed interface Order

class Before(val id: String): Order
class After(val id: String): Order

interface ApolloCompilerRegistry {
  fun registerForeignSchemas(schemas: List<ForeignSchema>)

  @ApolloExperimental
  fun registerExecutableDocumentTransform(id: String, vararg orders: Order, transform: ExecutableDocumentTransform)
  @ApolloExperimental
  fun registerIrTransform(id: String, vararg orders: Order, transform: Transform<IrOperations>)

  @ApolloExperimental
  fun registerLayout(factory: LayoutFactory)
  fun registerOperationIdsGenerator(generator: OperationIdsGenerator)
  @ApolloExperimental
  fun registerJavaOutputTransform(id: String, vararg orders: Order, transform: Transform<JavaOutput>)
  @ApolloExperimental
  fun registerKotlinOutputTransform(id: String, vararg orders: Order, transform: Transform<KotlinOutput>)
  @ApolloExperimental
  fun registerExtraCodeGenerator(codeGenerator: CodeGenerator)
}

/**
 * A code generator that may write code in [ApolloCompilerPluginEnvironment.outputDirectory]
 *
 * This is not a kotlin function type because this might be used in environments where those types are
 * relocated and might fail to load at runtime. For an example, in a Gradle plugin.
 */
fun interface CodeGenerator {
  /**
   * Transforms the given input into an output of the same type
   */
  fun generate(schema: GQLDocument, outputDirectory: File)
}