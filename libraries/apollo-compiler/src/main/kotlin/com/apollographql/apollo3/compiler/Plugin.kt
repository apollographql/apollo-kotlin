package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.Layout
import com.apollographql.apollo3.compiler.codegen.java.JavaOutput
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo3.compiler.ir.IrOperations

typealias JavaOutputTransform = ((JavaOutput) -> JavaOutput)
typealias KotlinOutputTransform = ((KotlinOutput) -> KotlinOutput)
typealias IrOperationsTransform = ((IrOperations) -> IrOperations)

interface Plugin {
  fun codegenLayout(codegenSchema: CodegenSchema): Layout? {
    return null
  }

  fun logger(): ApolloCompiler.Logger? {
    return null
  }

  fun operationOutputGenerator(): OperationOutputGenerator? {
    return null
  }

  fun irOperationsTransform(): IrOperationsTransform? {
    return null
  }

  fun javaOutputTransform(): JavaOutputTransform? {
    return null
  }

  fun kotlinOutputTransform(): KotlinOutputTransform? {
    return null
  }
}