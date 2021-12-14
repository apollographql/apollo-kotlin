package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.apolloDefinitions
import com.apollographql.apollo3.ast.validateAsSchema
import com.apollographql.apollo3.compiler.introspection.toGQLDocument
import com.apollographql.apollo3.compiler.introspection.toSchema
import com.apollographql.apollo3.compiler.introspection.toSchemaGQLDocument
import java.io.File


/**
 * These are the options that should be the same in all modules.
 */
class IncomingOptions(
    val schema: Schema,
    val codegenModels: String,
    val schemaPackageName: String,
) {
  companion object {
    fun fromMetadata(commonMetadata: CommonMetadata, packageNameGenerator: PackageNameGenerator): IncomingOptions {
      return IncomingOptions(
          schema = commonMetadata.schema,
          codegenModels = commonMetadata.codegenModels,
          schemaPackageName = packageNameGenerator.packageName(commonMetadata.schemaPath)
      )
    }

    @OptIn(ApolloExperimental::class)
    fun resolveSchema(schemaFiles: Collection<File>, rootFolders: List<String>): Pair<Schema, String> {
      check(schemaFiles.isNotEmpty()) {
        "No schema file found in:\n${rootFolders.joinToString("\n")}"
      }

      val schemaDocuments = schemaFiles.map {
        it.toSchemaGQLDocument()
      }

      // Locate the mainSchemaDocument. It's the one that contains the operation roots
      val mainSchemaDocuments = schemaDocuments.filter {
        it.definitions.filterIsInstance<GQLSchemaDefinition>().isNotEmpty()
            || it.definitions.filterIsInstance<GQLTypeDefinition>().any { it.name == "Query" }
      }

      check(mainSchemaDocuments.size == 1) {
        "Multiple schemas found:\n${mainSchemaDocuments.map { it.filePath }.joinToString("\n")}\n" +
            "Use different services for different schemas"
      }
      val mainSchemaDocument = mainSchemaDocuments.single()

      val schemaDefinitions = schemaDocuments.flatMap { it.definitions }
      val schemaDocument = GQLDocument(
          definitions = schemaDefinitions + apolloDefinitions(),
          filePath = null
      )

      return schemaDocument.validateAsSchema().valueAssertNoErrors() to mainSchemaDocument.filePath!!
    }
  }
}