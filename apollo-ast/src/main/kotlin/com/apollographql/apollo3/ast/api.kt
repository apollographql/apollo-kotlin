@file:JvmName("ApolloParser")
package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.internal.ExecutableValidationScope
import com.apollographql.apollo3.ast.internal.SchemaValidationScope
import com.apollographql.apollo3.ast.internal.antlrParse
import com.apollographql.apollo3.ast.internal.toGQLDocument
import com.apollographql.apollo3.ast.internal.toGQLSelection
import com.apollographql.apollo3.ast.internal.toGQLValue
import com.apollographql.apollo3.ast.internal.validateDocumentAndMergeExtensions
import okio.BufferedSource

/**
 * Parses the source to a [Schema], throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsSchema] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toSchema(filePath: String? = null): Schema = parseAsGQLDocument(filePath).valueAssertNoErrors().validateAsSchema().valueAssertNoErrors()

/**
 * Parses the source to a List<[GQLDefinition]>, throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsExecutable] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toExecutableDefinitions(schema: Schema, filePath: String? = null): List<GQLDefinition> = parseAsGQLDocument(filePath)
    .valueAssertNoErrors()
    .validateAsExecutable(schema)
    .valueAssertNoErrors()

/**
 * Parses the source to a [GQLDocument], validating the syntax but not the contents of the document.
 *
 * You can then use [validateAsSchema] to validate the contents and get a [Schema].
 * Or use [validateAsExecutable] to validate the contents get a list of operations/fragments.
 *
 * @return a [GQLResult] with either a non-null [GQLDocument] or a list of issues.
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLDocument(filePath: String? = null): GQLResult<GQLDocument> = use { source ->
  return antlrParse(source, filePath, { it.document() }, { it.toGQLDocument(filePath) })
}

/**
 * Parses the source to a [GQLValue], validating the syntax but not the contents of the value.
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLValue(filePath: String? = null): GQLResult<GQLValue> = use { source ->
  return antlrParse(source, filePath, { it.value() }, { it.toGQLValue(filePath) })
}

/**
 * Parses the source to a [List]<[GQLSelection]>, validating the syntax but not the contents of the selections.
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLSelections(filePath: String? = null): GQLResult<List<GQLSelection>> = use { source ->
  return antlrParse(source, filePath, { it.selections() }, { it.selection().map { it.toGQLSelection(filePath) } })
}

/**
 * - Validate the given document as a schema.
 * - Add a schema definition if there is none
 * - Merge type extensions
 *
 * @receiver the input document to validate and merge. It should not contain any builtin types
 * The current validation is very simple and will only catch simple errors
 *
 * @return a [GQLResult] containing the schema and any potential issues
 */
@ApolloExperimental
fun GQLDocument.validateAsSchema(): GQLResult<Schema> {
  val scope = SchemaValidationScope(this)
  val mergedDefinitions = scope.validateDocumentAndMergeExtensions()

  /**
   * If there is an error, do not try to instantiate a `Schema` as it will fail
   *
   * It might be that there are warnings though. For an example unknown directives
   * In that case, it is safe
   */
  val schema = if (scope.issues.containsError()) {
    null
  } else {
    Schema(mergedDefinitions)
  }
  return GQLResult(schema, scope.issues)
}

/**
 * Validates the given document as an executable document.
 *
 * @return  a [GQLResult] containing the operation and fragment definitions in 'value', along with any potential issues
 */
@ApolloExperimental
fun GQLDocument.validateAsExecutable(schema: Schema): GQLResult<List<GQLDefinition>> {
  val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
  val issues = ExecutableValidationScope(schema, fragments).validate(this)
  return GQLResult(definitions, issues)
}

/**
 * Infers the variables from a given fragment
 */
@ApolloExperimental
fun GQLFragmentDefinition.inferVariables(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
) = ExecutableValidationScope(schema, fragments).inferFragmentVariables(this)


