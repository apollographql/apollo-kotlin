package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.ir.Operation
import java.io.File

internal class TransformedQueryOutput(
    private val packageNameProvider: PackageNameProvider
) {
  private var transformedQueries: List<TransformedQuery> = emptyList()

  fun visit(operations: List<Operation>) {
    transformedQueries = transformedQueries + operations.map { operation ->
      val targetPackage = packageNameProvider.operationPackageName(operation.filePath)
      TransformedQuery(
          queryName = operation.operationName,
          queryDocument = operation.sourceWithFragments!!,
          packageName = targetPackage
      )
    }
  }

  fun writeTo(outputDir: File) {
    transformedQueries.forEach { transformedQuery ->
      outputDir.resolve(transformedQuery.packageName.replace('.', File.separatorChar)).run {
        mkdirs()
        resolve("${transformedQuery.queryName}.graphql")
      }.writeText(transformedQuery.queryDocument)
    }
  }

  private class TransformedQuery(
      val queryName: String,
      val queryDocument: String,
      val packageName: String
  )
}
