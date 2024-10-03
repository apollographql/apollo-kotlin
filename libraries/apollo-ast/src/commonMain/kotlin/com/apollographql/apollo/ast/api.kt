@file:JvmMultifileClass
@file:JvmName("ApolloParser")

package com.apollographql.apollo.ast

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.internal.ExecutableValidationScope
import com.apollographql.apollo.ast.internal.LexerException
import com.apollographql.apollo.ast.internal.Parser
import com.apollographql.apollo.ast.internal.ParserException
import com.apollographql.apollo.ast.internal.SchemaValidationOptions
import com.apollographql.apollo.ast.internal.validateSchema
import okio.BufferedSource
import okio.use
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Parses the source to a List<[GQLDefinition]>, throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsExecutable] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toExecutableDocument(
    schema: Schema,
    filePath: String? = null,
): GQLDocument = parseAsGQLDocument(filePath)
    .getOrThrow()
    .also {
      it.validateAsExecutable(schema).issues.checkValidGraphQL()
    }

private fun <T : Any> BufferedSource.parseInternal(filePath: String?, options: ParserOptions, block: Parser.() -> T): GQLResult<T> {
  return this.use { readUtf8() }.parseInternal(filePath, options, block)
}

private fun <T : Any> String.parseInternal(filePath: String?, options: ParserOptions, block: Parser.() -> T): GQLResult<T> {
  return try {
    GQLResult(Parser(this, options, filePath).block(), emptyList())
  } catch (e: ParserException) {
    GQLResult(
        null,
        listOf(
            ParsingError(
                e.message,
                SourceLocation(
                    start = e.token.start,
                    end = e.token.end,
                    line = e.token.line,
                    column = e.token.column,
                    filePath = filePath
                )
            )
        )
    )
  } catch (e: LexerException) {
    GQLResult(
        null,
        listOf(
            ParsingError(
                e.message,
                SourceLocation(
                    start = e.pos,
                    end = e.pos + 1,
                    line = e.line,
                    column = e.column,
                    filePath = filePath
                )
            )
        )
    )
  }
}

class ParserOptions private constructor(
    val allowEmptyDocuments: Boolean,
    val withSourceLocation: Boolean,
) {
  class Builder {
    var allowEmptyDocuments = true
    var withSourceLocation = true

    fun allowEmptyDocuments(allowEmptyDocuments: Boolean) = apply {
      this.allowEmptyDocuments = allowEmptyDocuments
    }

    fun withSourceLocation(withSourceLocation: Boolean) = apply {
      this.withSourceLocation = withSourceLocation
    }

    fun build(): ParserOptions {
      return ParserOptions(
          allowEmptyDocuments = allowEmptyDocuments,
          withSourceLocation = withSourceLocation
      )
    }
  }

  companion object {
    val Default = Builder().build()
  }
}

fun String.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return parseInternal(null, options) { parseDocument() }
}

fun String.toGQLDocument(options: ParserOptions = ParserOptions.Default): GQLDocument {
  return parseAsGQLDocument(options).getOrThrow()
}

fun String.parseAsGQLValue(options: ParserOptions = ParserOptions.Default): GQLResult<GQLValue> {
  return parseInternal(null, options) { parseValue() }
}

fun String.toGQLValue(options: ParserOptions = ParserOptions.Default): GQLValue {
  return parseAsGQLValue(options).getOrThrow()
}

fun String.parseAsGQLType(options: ParserOptions = ParserOptions.Default): GQLResult<GQLType> {
  return parseInternal(null, options) { parseType() }
}

fun String.toGQLType(options: ParserOptions = ParserOptions.Default): GQLType {
  return parseAsGQLType(options).getOrThrow()
}

fun String.parseAsGQLSelections(options: ParserOptions = ParserOptions.Default): GQLResult<List<GQLSelection>> {
  return parseInternal(null, options) { parseSelections() }
}

fun String.toGQLSelections(options: ParserOptions = ParserOptions.Default): List<GQLSelection> {
  return parseAsGQLSelections(options).getOrThrow()
}

/**
 * Parses the source to a [GQLDocument], validating the syntax but not the contents of the document.
 *
 * You can then use [validateAsSchema] to validate the contents and get a [Schema].
 * Or use [validateAsExecutable] to validate the contents get a list of operations/fragments.
 *
 * Closes [BufferedSource]
 *
 * @return a [GQLResult] with either a non-null [GQLDocument] or a list of issues.
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLDocument(filePath: String? = null, options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return parseInternal(filePath, options) { parseDocument() }
}

/**
 * Parses the source to a [GQLValue], validating the syntax but not the contents of the value.
 *
 * Closes [BufferedSource]
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLValue(filePath: String? = null, options: ParserOptions = ParserOptions.Default): GQLResult<GQLValue> {
  return parseInternal(filePath, options) { parseValue() }
}

/**
 * Parses the source to a [GQLType], validating the syntax but not the contents of the value.
 *
 * Closes [BufferedSource]
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLType(filePath: String? = null, options: ParserOptions = ParserOptions.Default): GQLResult<GQLType> {
  return parseInternal(filePath, options) { parseType() }
}

/**
 * Parses the source to a [List]<[GQLSelection]>, validating the syntax but not the contents of the selections.
 *
 * Closes [BufferedSource]
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLSelections(
    filePath: String? = null,
    options: ParserOptions = ParserOptions.Default,
): GQLResult<List<GQLSelection>> {
  return parseInternal(filePath, options) { parseSelections() }
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
  return validateSchema(definitions, SchemaValidationOptions(false, emptyList()))
}

@ApolloExperimental
fun GQLDocument.validateAsSchema(validationOptions: SchemaValidationOptions): GQLResult<Schema> {
  return validateSchema(definitions, validationOptions)
}

@ApolloInternal
fun GQLDocument.validateAsSchemaAndAddApolloDefinition(): GQLResult<Schema> {
  return validateSchema(
      definitions,
      SchemaValidationOptions(
          true,
          builtinForeignSchemas()
      )
  )
}

/**
 * Validates the given document as an executable document.
 *
 * @return  a [GQLResult] containing the operation and fragment definitions in 'value', along with any potential issues
 */
@ApolloExperimental
fun GQLDocument.validateAsExecutable(schema: Schema): ExecutableValidationResult {
  return ExecutableValidationScope(schema).validate(this)
}

@ApolloExperimental
class ExecutableValidationResult(val fragmentVariableUsages: Map<String, List<VariableUsage>>, val issues: List<Issue>)
