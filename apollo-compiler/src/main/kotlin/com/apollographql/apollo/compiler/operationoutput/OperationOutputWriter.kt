package com.apollographql.apollo.compiler.operationoutput

import com.apollographql.apollo.api.internal.QueryDocumentMinifier
import com.apollographql.apollo.compiler.OperationIdGenerator
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.Operation
import java.io.File

internal class OperationOutputWriter(private val operationIdGenerator: OperationIdGenerator) {
  private var operations: List<Operation> = emptyList()

  fun visit(ir: CodeGenerationIR) {
    operations = operations + ir.operations
  }

  fun writeTo(outputJsonFile: File) {
    val operationOutput = operations.associate {
      val minimizedSource = QueryDocumentMinifier.minify(it.sourceWithFragments)
      operationIdGenerator.apply(minimizedSource, it.filePath) to OperationDescriptor(it.operationName, minimizedSource)
    }

    outputJsonFile.writeText(operationOutput.toJson("    "))
  }
}
