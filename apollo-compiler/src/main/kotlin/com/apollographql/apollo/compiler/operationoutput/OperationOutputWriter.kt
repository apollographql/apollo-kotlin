package com.apollographql.apollo.compiler.operationoutput

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.compiler.sha256
import com.apollographql.apollo.internal.QueryDocumentMinifier
import java.io.File
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

internal class OperationOutputWriter(val packageNameProvider: PackageNameProvider) {
  private var operations: List<Operation> = emptyList()

  fun visit(ir: CodeGenerationIR) {
    operations = operations + ir.operations
  }

  fun writeTo(outputJsonFile: File) {
    val operationOuput = operations.map {
      val minimizedSource = QueryDocumentMinifier.minify(it.sourceWithFragments)
      minimizedSource.sha256() to OperationDescriptor(it.operationName, minimizedSource)
    }.toMap()

    outputJsonFile.writeText(operationOuput.toJson("    "))
  }
}
