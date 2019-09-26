package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.CodeGenerationIR
import java.io.File

internal class TransformedQueryOutput(
    private val packageNameProvider: PackageNameProvider
) {
  private var transformedDocuments: List<TransformedDocument> = emptyList()

  fun visit(ir: CodeGenerationIR) {
    transformedDocuments = transformedDocuments + ir.operations.map { operation ->
      val targetPackage = packageNameProvider.operationPackageName(operation.filePath)
      TransformedDocument(
          name = operation.operationName,
          document = operation.source,
          packageName = targetPackage
      )
    }
    transformedDocuments = transformedDocuments + ir.fragments.map { fragment ->
      val targetPackage = packageNameProvider.fragmentsPackageName
      TransformedDocument(
          name = fragment.fragmentName,
          document = fragment.source,
          packageName = targetPackage
      )
    }
  }

  fun writeTo(outputDir: File) {
    transformedDocuments.forEach { transformedDocument ->
      outputDir.resolve(transformedDocument.packageName.replace('.', File.separatorChar)).run {
        mkdirs()
        resolve("${transformedDocument.name}.graphql")
      }.writeText(transformedDocument.document)
    }
  }

  private class TransformedDocument(
      val name: String,
      val document: String,
      val packageName: String
  )
}
