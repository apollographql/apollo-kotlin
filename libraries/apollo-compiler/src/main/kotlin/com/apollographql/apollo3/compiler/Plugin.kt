package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo3.compiler.codegen.java.JavaOutput
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo3.compiler.ir.IrOperations
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationId

/**
 * Entry point for customizing the behaviour of the Apollo Compiler besides the
 * already existing options
 */
interface Plugin {
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
