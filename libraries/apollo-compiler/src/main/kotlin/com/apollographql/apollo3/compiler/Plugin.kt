package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.SchemaAndOperationsLayout
import com.apollographql.apollo3.compiler.codegen.java.JavaOutput
import com.apollographql.apollo3.compiler.codegen.kotlin.KotlinOutput
import com.apollographql.apollo3.compiler.ir.IrOperations

interface Transform<T> {
  fun transform(t: T): T
}

interface Plugin {
  fun layout(codegenSchema: CodegenSchema): SchemaAndOperationsLayout? {
    return null
  }

  fun operationOutputGenerator(): OperationOutputGenerator? {
    return null
  }

  fun irOperationsTransform(): Transform<IrOperations>? {
    return null
  }

  fun javaOutputTransform(): Transform<JavaOutput>? {
    return null
  }

  fun kotlinOutputTransform(): Transform<KotlinOutput>? {
    return null
  }
}