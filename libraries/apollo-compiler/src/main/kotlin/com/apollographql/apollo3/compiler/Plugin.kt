package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo3.compiler.codegen.java.JavaOutput
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo3.compiler.ir.IrOperations

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
   * @return the [OperationOutputGenerator] or null to use the default [OperationOutputGenerator]
   */
  fun operationOutputGenerator(): OperationOutputGenerator? {
    return null
  }

  /**
   * @return the [Transform] to be applied to [IrOperations] or null to use the default [Transform]
   */
  fun irOperationsTransform(): Transform<IrOperations>? {
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
