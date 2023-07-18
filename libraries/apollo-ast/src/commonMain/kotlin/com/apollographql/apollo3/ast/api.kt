@file:JvmMultifileClass
@file:JvmName("ApolloParser")

package com.apollographql.apollo3.ast

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.internal.ExecutableValidationScope
import com.apollographql.apollo3.ast.internal.LexerException
import com.apollographql.apollo3.ast.internal.Parser
import com.apollographql.apollo3.ast.internal.ParserException
import com.apollographql.apollo3.ast.internal.validateSchema
import okio.Buffer
import okio.BufferedSource
import okio.Path
import okio.buffer
import okio.use
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Parses the source to a [Schema], throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsSchema] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toSchema(filePath: String? = null): Schema = parseAsGQLDocument(filePath).getOrThrow().validateAsSchema().getOrThrow()

@ApolloExperimental
fun String.toSchema(): Schema = parseAsGQLDocument().getOrThrow().validateAsSchema().getOrThrow()

/**
 * Parses the source to a List<[GQLDefinition]>, throwing on parsing or validation errors.
 *
 * See [parseAsGQLDocument] and [validateAsExecutable] for more granular error reporting
 */
@ApolloExperimental
fun BufferedSource.toExecutableDefinitions(
    schema: Schema,
    filePath: String? = null,
    fieldsOnDisjointTypesMustMerge: Boolean = true,
): List<GQLDefinition> = parseAsGQLDocument(filePath)
    .getOrThrow()
    .validateAsExecutable(schema, fieldsOnDisjointTypesMustMerge)
    .getOrThrow()

private fun <T : Any> BufferedSource.parseInternal(filePath: String?, withSourceLocation: Boolean, block: Parser.() -> T): GQLResult<T> {
  return this.use { readUtf8() }.parseInternal(filePath, withSourceLocation, block)
}

private fun <T : Any> String.parseInternal(filePath: String?, withSourceLocation: Boolean, block: Parser.() -> T): GQLResult<T> {
  return try {
    GQLResult(Parser(this, withSourceLocation, filePath).block(), emptyList())
  } catch (e: ParserException) {
    GQLResult(
        null,
        listOf(
            Issue.ParsingError(
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
            Issue.ParsingError(
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

class ParserOptions(
    @Deprecated("This is used as a fallback the time to stabilize the new parser but will be removed")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    val useAntlr: Boolean = false,
    val allowEmptyDocuments: Boolean = true,
    val withSourceLocation: Boolean = true,
) {
  companion object {
    val Default = ParserOptions()
  }
}

internal expect fun parseDocumentWithAntlr(source: BufferedSource, filePath: String?): GQLResult<GQLDocument>
internal expect fun parseValueWithAntlr(source: BufferedSource, filePath: String?): GQLResult<GQLValue>
internal expect fun parseTypeWithAntlr(source: BufferedSource, filePath: String?): GQLResult<GQLType>
internal expect fun parseSelectionsWithAntlr(source: BufferedSource, filePath: String?): GQLResult<List<GQLSelection>>

fun String.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    Buffer().writeUtf8(this).parseAsGQLDocument(options = options)
  } else {
    parseInternal(null, options.withSourceLocation) { parseDocument(options.allowEmptyDocuments) }
  }
}
fun String.parseAsGQLValue(options: ParserOptions = ParserOptions.Default): GQLResult<GQLValue> {
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    Buffer().writeUtf8(this).parseAsGQLValue(options = options)
  } else {
    parseInternal(null, options.withSourceLocation) { parseValue() }
  }
}
fun String.parseAsGQLType(options: ParserOptions = ParserOptions.Default): GQLResult<GQLType> {
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    Buffer().writeUtf8(this).parseAsGQLType(options = options)
  } else {
    parseInternal(null, options.withSourceLocation) { parseType() }
  }
}
fun String.parseAsGQLSelections(options: ParserOptions = ParserOptions.Default): GQLResult<List<GQLSelection>> {
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    Buffer().writeUtf8(this).parseAsGQLSelections(options = options)
  } else {
    parseInternal(null, options.withSourceLocation) { parseSelections() }
  }
}

fun Path.parseAsGQLDocument(options: ParserOptions = ParserOptions.Default): GQLResult<GQLDocument> {
  return HOST_FILESYSTEM.source(this).buffer().parseAsGQLDocument(filePath = toString(), options = options)
}
fun Path.parseAsGQLValue(options: ParserOptions = ParserOptions.Default): GQLResult<GQLValue> {
  return HOST_FILESYSTEM.source(this).buffer().parseAsGQLValue(filePath = toString(), options = options)
}
fun Path.parseAsGQLType(options: ParserOptions = ParserOptions.Default): GQLResult<GQLType> {
  return HOST_FILESYSTEM.source(this).buffer().parseAsGQLType(filePath = toString(), options = options)
}
fun Path.parseAsGQLSelections(options: ParserOptions = ParserOptions.Default): GQLResult<List<GQLSelection>> {
  return HOST_FILESYSTEM.source(this).buffer().parseAsGQLSelections(filePath = toString(), options = options)
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
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    parseDocumentWithAntlr(this, filePath)
  } else {
    parseInternal(filePath, options.withSourceLocation) { parseDocument(options.allowEmptyDocuments) }
  }
}

/**
 * Parses the source to a [GQLValue], validating the syntax but not the contents of the value.
 *
 * Closes [BufferedSource]
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLValue(filePath: String? = null, options: ParserOptions = ParserOptions.Default): GQLResult<GQLValue> {
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    parseValueWithAntlr(this, filePath)
  } else {
    parseInternal(filePath, options.withSourceLocation) { parseValue() }
  }
}

/**
 * Parses the source to a [GQLType], validating the syntax but not the contents of the value.
 *
 * Closes [BufferedSource]
 */
@ApolloExperimental
fun BufferedSource.parseAsGQLType(filePath: String? = null, options: ParserOptions = ParserOptions.Default): GQLResult<GQLType> {
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    parseTypeWithAntlr(this, filePath)
  } else {
    parseInternal(filePath, options.withSourceLocation) { parseType() }
  }
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
  @Suppress("DEPRECATION")
  return if (options.useAntlr) {
    parseSelectionsWithAntlr(this, filePath)
  } else {
    parseInternal(filePath, options.withSourceLocation) { parseSelections() }
  }
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
    fieldsOnDisjointTypesMustMerge: Boolean,
) = ExecutableValidationScope(schema, fragments, fieldsOnDisjointTypesMustMerge).inferFragmentVariables(this)


