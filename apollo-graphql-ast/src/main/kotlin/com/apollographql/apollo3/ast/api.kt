package com.apollographql.apollo3.ast

import com.apollographql.apollo3.ast.internal.ExecutableValidationScope
import com.apollographql.apollo3.ast.internal.SchemaValidationScope
import com.apollographql.apollo3.ast.internal.antlrParse
import com.apollographql.apollo3.ast.internal.toGQLDocument
import com.apollographql.apollo3.ast.internal.toGQLSelection
import com.apollographql.apollo3.ast.internal.toGQLValue
import com.apollographql.apollo3.ast.internal.validateDocumentAndMergeExtensions
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File


/**
 * Parses a GraphQL document to a [GQLDocument], validating the syntax but not the contents of the document.
 *
 * Use [toSchema] to parse and validate a [Schema] in one call.
 * Use [toExecutableGQLDefinitions] to parse and validate an executable document containing one or several
 * operation in one call.
 *
 * @return a [ParseResult] with either a non-null [GQLDocument] or a list of issues.
 */
fun BufferedSource.parseAsGQLDocument(filePath: String? = null): ParseResult<GQLDocument> = use { source ->
  return antlrParse(source, filePath, { it.document() }, { it.toGQLDocument(filePath) })
}

/**
 * See [parseAsGQLDocument]
 */
fun File.parseAsGQLDocument() = source().buffer().parseAsGQLDocument(absolutePath)

/**
 * See [parseAsGQLDocument]
 */
fun String.parseAsGQLDocument() = byteInputStream().source().buffer().parseAsGQLDocument()

/**
 * Parses the [BufferedSource] into a [GQLDocument]
 *
 * Throw if the document syntax is not correct but doesn't do any additional validation
 *
 */
fun BufferedSource.toGQLDocument(filePath: String? = null) = parseAsGQLDocument(filePath).getOrThrow()

/**
 * See [toGQLDocument]
 */
fun File.toGQLDocument() = parseAsGQLDocument().getOrThrow()

/**
 * See [toGQLDocument]
 */
fun String.toGQLDocument() = parseAsGQLDocument().getOrThrow()



/**
 * Parses a GraphQL value to a [GQLValue], validating the syntax but not the contents of the value.
 */
fun BufferedSource.parseAsGQLValue(filePath: String? = null): ParseResult<GQLValue> = use { source ->
  return antlrParse(source, filePath, { it.value() }, { it.toGQLValue(filePath) })
}

/**
 * See [parseAsGQLValue]
 */
fun File.parseAsGQLValue() = source().buffer().parseAsGQLValue(absolutePath)

/**
 * See [parseAsGQLValue]
 */
fun String.parseAsGQLValue() = byteInputStream().source().buffer().parseAsGQLValue()

/**
 * Parses the [BufferedSource] into a [GQLValue]
 *
 * Throw if the document syntax is not correct but doesn't do any additional validation
 */
fun BufferedSource.toGQLValue(filePath: String? = null) = parseAsGQLValue(filePath).getOrThrow()

/**
 * See [toGQLValue]
 */
fun File.toGQLValue() = parseAsGQLValue().getOrThrow()

/**
 * See [toGQLValue]
 */
fun String.toGQLValue() = parseAsGQLValue().getOrThrow()



/**
 * Parses a list of GraphQL selections to a [List]<[GQLSelection]>, validating the syntax but not the contents of the selections.
 */
fun BufferedSource.parseAsGQLSelections(filePath: String? = null): ParseResult<List<GQLSelection>> = use { source ->
  return antlrParse(source, filePath, { it.selections() }, { it.selection().map { it.toGQLSelection(filePath) } })
}

/**
 * See [parseAsGQLSelections]
 */
fun File.parseAsGQLSelections() = source().buffer().parseAsGQLSelections(absolutePath)

/**
 * See [parseAsGQLSelections]
 */
fun String.parseAsGQLSelections() = byteInputStream().source().buffer().parseAsGQLSelections()

/**
 * Parses the [BufferedSource] into a [GQLValue]
 *
 * Throw if the document syntax is not correct but doesn't do any additional validation
 */
fun BufferedSource.toGQLSelections(filePath: String? = null) = parseAsGQLSelections(filePath).getOrThrow()

/**
 * See [toGQLValue]
 */
fun File.toGQLSelections() = parseAsGQLSelections().getOrThrow()

/**
 * See [toGQLValue]
 */
fun String.toGQLSelections() = parseAsGQLSelections().getOrThrow()





/**
 * - Validate the given document as a schema.
 * - Add a schema definition if there is none
 * - Merge type extensions
 *
 * @receiver the input document to validate and merge. It should not contain any builtin types
 * The current validation is very simple and will only catch simple errors
 */
fun GQLDocument.validateAsSchema(): List<Issue> {
  val scope = SchemaValidationScope(this)
  scope.validateDocumentAndMergeExtensions()
  return scope.issues
}

/**
 * Validates the given document as an executable document.
 */
fun GQLDocument.validateAsExecutable(schema: Schema): List<Issue> {
  val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
  return ExecutableValidationScope(schema, fragments).validate(this)
}

/**
 * Infers the variables from a given fragment
 */
fun GQLFragmentDefinition.inferVariables(
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
) = ExecutableValidationScope(schema, fragments).inferFragmentVariables(this)




/**
 * Validates the given [GQLDocument] and returns a [Schema] with type extensions merged
 *
 * - Validate the given document as a schema.
 * - Add a schema definition if there is none
 * - Merge type extensions
 */
fun GQLDocument.toSchema(): Schema {
  val scope = SchemaValidationScope(this)
  val mergedDefinitions = scope.validateDocumentAndMergeExtensions()
  scope.issues.checkNoErrors()
  return Schema(mergedDefinitions)
}

/**
 * Parses a the [BufferedSource] to a [Schema] and validates the result.
 *
 * Throws if the input is not a valid schema.
 *
 * For more fine grained control, see [parseAsGQLDocument]
 */
fun BufferedSource.toSchema(filePath: String? = null) = toGQLDocument(filePath).toSchema()

/**
 * See [toSchema]
 */
fun File.toSchema() = source().buffer().toSchema(absolutePath)

/**
 * See [toSchema]
 */
fun String.toSchema() = byteInputStream().source().buffer().toSchema()

/**
 * Parses a the [BufferedSource] to a List<[GQLDefinition]> and validates the result
 *
 * throws if the input is not a valid executable document
 *
 * For more fine grained control, look at [parseAsGQLDocument]
 *
 * @param schema a [Schema] used to validate the operations and fragments
 * @param filePath an optional path that will be displayed in errors for better troubleshooting
 */
fun BufferedSource.toExecutableGQLDefinitions(schema: Schema, filePath: String? = null): List<GQLDefinition> {
  return parseAsGQLDocument(filePath).getOrThrow().also {
    it.validateAsExecutable(schema)
  }.definitions
}

/**
 * See [toExecutableGQLDefinitions]
 */
fun File.toExecutableGQLDefinitions(schema: Schema) = source().buffer().toExecutableGQLDefinitions(schema, absolutePath)

/**
 * See [toExecutableGQLDefinitions]
 */
fun String.toExecutableGQLDefinitions(schema: Schema) = byteInputStream().source().buffer().toExecutableGQLDefinitions(schema)
