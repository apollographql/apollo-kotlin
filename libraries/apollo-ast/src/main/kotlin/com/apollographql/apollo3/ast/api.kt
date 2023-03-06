@file:JvmName("ApolloParser")

package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.internal.ExecutableValidationScope
import com.apollographql.apollo3.ast.internal.antlrParse
import com.apollographql.apollo3.ast.internal.toGQLDocument
import com.apollographql.apollo3.ast.internal.toGQLSelection
import com.apollographql.apollo3.ast.internal.toGQLType
import com.apollographql.apollo3.ast.internal.toGQLValue
import com.apollographql.apollo3.ast.internal.validateSchema
import okio.BufferedSource

/**
 * Parses the source to a [Schema], throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsSchema] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toSchema(filePath: String? = null): Schema = parseAsGQLDocument(filePath).getOrThrow().validateAsSchema().getOrThrow()

/**
 * Parses the source to a List<[GQLDefinition]>, throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsExecutable] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toExecutableDefinitions(schema: Schema, filePath: String? = null, fieldsOnDisjointTypesMustMerge: Boolean = true): List<GQLDefinition> = parseAsGQLDocument(filePath)
    .getOrThrow()
    .validateAsExecutable(schema, fieldsOnDisjointTypesMustMerge)
    .getOrThrow()

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
 * Parses the source to a [GQLType], validating the syntax but not the contents of the value.
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLType(filePath: String? = null): GQLResult<GQLType> = use { source ->
  return antlrParse(source, filePath, { it.type() }, { it.toGQLType(filePath) })
}

/**
 * Parses the source to a [List]<[GQLSelection]>, validating the syntax but not the contents of the selections.
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLSelections(filePath: String? = null): GQLResult<List<GQLSelection>> = use { source ->
  return antlrParse(source, filePath, { it.selections() }, { it.selection().map { it.toGQLSelection(filePath) } })
}

/**
 * Validate the given document as a schema:
 * - Add the builtin definitions if they are not present already
 * - Process any `@link` directive and imports definition if any
 * - ensure uniqueness of schema/types/directives definitions
 * - Merge type extensions
 *
 * Although some validation is performed, this function does not pretend to implement the full GraphQL validation rules.
 *
 * @receiver the input document to validate and merge. It should not contain any builtin types
 * The current validation is very simple and will only catch simple errors
 *
 * @return a [GQLResult] containing the schema and any potential issues
 */
@ApolloExperimental
fun GQLDocument.validateAsSchema(): GQLResult<Schema> {
  return validateSchema(definitions)
}

@ApolloInternal
fun GQLDocument.validateAsSchemaAndAddApolloDefinition(): GQLResult<Schema> {
  return validateSchema(definitions, true)
}

/**
 * Validates the given document as an executable document.
 *
 * @param fieldsOnDisjointTypesMustMerge set to false to relax the standard GraphQL [FieldsInSetCanMerge](https://spec.graphql.org/draft/#FieldsInSetCanMerge())
 * and allow fields of different types at the same Json path as long as their parent types are disjoint.
 *
 * @return  a [GQLResult] containing the operation and fragment definitions in 'value', along with any potential issues
 */
@ApolloExperimental
fun GQLDocument.validateAsExecutable(schema: Schema, fieldsOnDisjointTypesMustMerge: Boolean = true): GQLResult<List<GQLDefinition>> {
  val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
  val issues = ExecutableValidationScope(schema, fragments, fieldsOnDisjointTypesMustMerge).validate(this)
  return GQLResult(definitions, issues)
}

/**
 * Infers the variables from a given fragment
 */
@ApolloExperimental
fun GQLFragmentDefinition.inferVariables(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
    fieldsOnDisjointTypesMustMerge: Boolean
) = ExecutableValidationScope(schema, fragments, fieldsOnDisjointTypesMustMerge).inferFragmentVariables(this)


