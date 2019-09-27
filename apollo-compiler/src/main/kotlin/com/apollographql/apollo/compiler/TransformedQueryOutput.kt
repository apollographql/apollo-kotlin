package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import java.io.File

internal class TransformedQueryOutput {
  private var transformedDocuments: List<TransformedDocument> = emptyList()

  fun visit(ir: CodeGenerationIR) {
    transformedDocuments = transformedDocuments + ir.operations.map { operation ->
      TransformedDocument(
          document = operation.source,
          filePath = operation.filePath.relativePathToGraphql()!!
      )
    }
    transformedDocuments = transformedDocuments + ir.fragments.map { fragment ->
      TransformedDocument(
          document = fragment.source,
          filePath = fragment.filePath.relativePathToGraphql()!!
      )
    }
  }

  fun writeTo(outputDir: File) {
    transformedDocuments
      .groupBy { it.filePath } // Multiple documents can be from the same file, e.g. fragments
      .forEach { (filePath, transformedDocuments) ->
        val outputFile = outputDir.resolve(filePath).also {
          it.parentFile.mkdirs()
        }
        transformedDocuments
          .joinToString(separator = "\n\n") { it.document }
          .also { outputFile.writeText(it) }
      }
  }

  private class TransformedDocument(
      val document: String,
      val filePath: String
  )
}
