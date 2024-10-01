package com.apollographql.apollo.compiler

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
 * [ApolloCompilerPlugin] allows to customize the behaviour of the Apollo Compiler.
 *
 * [ApolloCompilerPlugin] may be instantiated several times in a codegen run. Each instance is create in a
 * separate classloader.
 * The classloaders contains `apollo-compiler` classes and the runtime classpath of the [ApolloCompilerPlugin].
 * You may throw from [ApolloCompilerPlugin] methods to fail the build.
 */
interface ApolloCompilerPlugin {
  /**
   * @return the layout or null to use the default layout
   * @param codegenSchema the codegenSchema
   */
  fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
    return null
  }

  /**
   * Computes operation ids for persisted queries.
   *
   * @return a list of [OperationId] matching an operation name to its id or null to use the SHA256 default
   */
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
