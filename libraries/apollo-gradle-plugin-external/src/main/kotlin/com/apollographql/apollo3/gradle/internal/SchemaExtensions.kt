package com.apollographql.apollo3.gradle.internal

import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.introspection.toSchemaGQLDocument
import com.apollographql.apollo3.ast.validateAsSchemaAndAddApolloDefinition
import org.gradle.api.logging.Logger
import java.io.File

internal fun Iterable<File>.toSchema(logger: Logger): Schema {
  return map {
    it.toSchemaGQLDocument()
  }.flatMap {
    it.definitions
  }.let {
    /**
     * TODO: use `validateAsSchema` to not automatically add the apollo definitions
     */
    val result = GQLDocument(
        definitions = it,
        filePath = null
    ).validateAsSchemaAndAddApolloDefinition()

    result.issues.filter { it.severity == Issue.Severity.WARNING }.forEach {
      // Using this format, IntelliJ will parse the warning and display it in the 'run' panel
      logger.warn("w: ${it.sourceLocation.pretty()}: Apollo: ${it.message}")
    }

    result.getOrThrow()
  }
}
