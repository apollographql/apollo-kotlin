package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import com.apollographql.apollo.internal.QueryDocumentMinifier
import java.io.File

internal class TransformedQueryOutput(val packageNameProvider: PackageNameProvider) {
  private var transformedDocuments: List<TransformedDocument> = emptyList()

  fun visit(ir: CodeGenerationIR) {
    transformedDocuments = transformedDocuments + ir.operations.map { operation ->
      TransformedDocument(
          sourceWithFragments = operation.sourceWithFragments,
          filePath = operation.filePath
      )
    }
  }

  fun writeTo(outputDir: File) {
    transformedDocuments
      .forEach { transformedDocument ->
        val filename = transformedDocument.filePath.substringAfterLast(File.separator)
        val relativePath = packageNameProvider.operationPackageName(transformedDocument.filePath)
            .replace(".", File.separator)
            .let {
              it + File.separator + filename
            }
        val outputFile = outputDir.resolve(relativePath).also {
          it.parentFile.mkdirs()
        }
        outputFile.writeText(QueryDocumentMinifier.minify(transformedDocument.sourceWithFragments))
      }
  }

  private class TransformedDocument(
      val sourceWithFragments: String,
      val filePath: String
  )
}
