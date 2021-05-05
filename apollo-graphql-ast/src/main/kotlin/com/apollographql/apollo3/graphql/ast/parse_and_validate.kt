package com.apollographql.apollo3.graphql.ast

import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File



/**
 * Parses a the [BufferedSource] to a [Schema] and validates the result.
 *
 * Throws if the input is not a valid schema.
 *
 * For more fine grained control, look at [parseAsGraphQLDocument] and [validateAsSchema]
 */
fun BufferedSource.toGraphQLSchema(filePath: String? = null): Schema {
  val document = parseAsGraphQLDocument(filePath)
      .getOrThrow()

  document.validateAsSchema().checkNoErrors()

  return Schema(document)
}

/**
 * See [toGraphQLSchema]
 */
fun File.toGraphQLSchema() = source().buffer().toGraphQLSchema(absolutePath)

/**
 * See [toGraphQLSchema]
 */
fun String.toGraphQLSchema() = byteInputStream().source().buffer().toGraphQLSchema()

/**
 * Parses a the [BufferedSource] to a List<[GQLDefinition]> and validates the result
 *
 * throws if the input is not a valid executable document
 *
 * For more fine grained control, look at [parseAsGraphQLDocument] and [validateAsSchema]
 *
 * @param schema a [Schema] used to validate the operations and fragments
 * @param filePath an optional path that will be displayed in errors for better troubleshooting
 */
fun BufferedSource.toGraphQLExecutableDefinitions(schema: Schema, filePath: String? = null): List<GQLDefinition> {
  val document = parseAsGraphQLDocument(filePath)
      .getOrThrow()

  document.validateAsOperations(schema)

  return document.definitions
}

/**
 * See [toGraphQLExecutableDefinitions]
 */
fun File.toGraphQLExecutableDefinitions(schema: Schema) = source().buffer().toGraphQLExecutableDefinitions(schema, absolutePath)

/**
 * See [toGraphQLExecutableDefinitions]
 */
fun String.toGraphQLExecutableDefinitions(schema: Schema) = byteInputStream().source().buffer().toGraphQLExecutableDefinitions(schema)
