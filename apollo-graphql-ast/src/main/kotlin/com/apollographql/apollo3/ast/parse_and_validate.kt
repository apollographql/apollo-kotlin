package com.apollographql.apollo3.ast

import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File


/**
 * Parses a the [BufferedSource] to a [Schema] and validates the result.
 *
 * Throws if the input is not a valid schema.
 *
 * For more fine grained control, see [parseAsGQLDocument] and [validateAsSchemaAndMerge]
 */
fun BufferedSource.toSchema(filePath: String? = null): Schema {
  val document = parseAsGQLDocument(filePath)
      .getOrThrow()

  return document.toSchema()
}

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
 * For more fine grained control, look at [parseAsGQLDocument] and [validateAsSchemaAndMerge]
 *
 * @param schema a [Schema] used to validate the operations and fragments
 * @param filePath an optional path that will be displayed in errors for better troubleshooting
 */
fun BufferedSource.toExecutableGQLDefinitions(schema: Schema, filePath: String? = null): List<GQLDefinition> {
  val document = parseAsGQLDocument(filePath)
      .getOrThrow()

  document.validateAsOperations(schema)

  return document.definitions
}

/**
 * See [toExecutableGQLDefinitions]
 */
fun File.toExecutableGQLDefinitions(schema: Schema) = source().buffer().toExecutableGQLDefinitions(schema, absolutePath)

/**
 * See [toExecutableGQLDefinitions]
 */
fun String.toExecutableGQLDefinitions(schema: Schema) = byteInputStream().source().buffer().toExecutableGQLDefinitions(schema)
