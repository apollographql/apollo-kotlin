package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
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
    fun resolveSchema(schemaFiles: Collection<File>): Pair<Schema, String> {
      check(schemaFiles.isNotEmpty()) {
        "No schema file found\n"
      }

      val schemaDocuments = schemaFiles.map {
        it.toSchemaGQLDocument()
      }

      // Locate the mainSchemaDocument. It's the one that contains the operation roots
      val mainSchemaDocuments = schemaDocuments.filter {
        it.definitions.filterIsInstance<GQLSchemaDefinition>().isNotEmpty()
            || it.definitions.filterIsInstance<GQLTypeDefinition>().any { it.name == "Query" }
      }

      if (mainSchemaDocuments.size > 1) {
        error("Multiple schemas found:\n${mainSchemaDocuments.map { it.filePath }.joinToString("\n")}\n" +
            "Use different services for different schemas")
      } else if (mainSchemaDocuments.isEmpty()) {
        error("Schema(s) found:\n${schemaFiles.map { it.absolutePath }.joinToString("\n")}\n" +
            "But none of them contain type definitions.")
      }
      val mainSchemaDocument = mainSchemaDocuments.single()

      val schemaDefinitions = schemaDocuments.flatMap { it.definitions }
      val schemaDocument = GQLDocument(
          definitions = schemaDefinitions,
          filePath = null
      )

      /**
       * TODO: use `validateAsSchema` to not automatically add the apollo definitions
       */
      val result = schemaDocument.validateAsSchemaAndAddApolloDefinition()

      result.issues.filter { it.severity == Issue.Severity.WARNING }.forEach {
        // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
        println("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
      }
      return result.getOrThrow() to mainSchemaDocument.filePath!!
    }
  }
}
