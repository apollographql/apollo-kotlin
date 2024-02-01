package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo3.compiler.codegen.java.JavaOutput
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo3.compiler.ir.IrOperations

typealias JavaOutputTransform = ((JavaOutput) -> JavaOutput)
typealias KotlinOutputTransform = ((KotlinOutput) -> KotlinOutput)
typealias IrOperationsTransform = ((IrOperations) -> IrOperations)

interface Plugin {
  fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
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