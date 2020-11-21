package com.apollographql.apollo.compiler.parser.gql

import java.io.File
import java.io.IOException

open class SourceAwareException(
    message: String,
    sourceLocation: SourceLocation,
) : RuntimeException(preview(
    message = message,
    sourceLocation = sourceLocation
)) {

  companion object {
    private fun preview(message: String, sourceLocation: SourceLocation): String {
      val filePath = sourceLocation.filePath
      return when {
        sourceLocation == SourceLocation.UNKNOWN -> "\nFailed to parse $filePath:\n$message"
        filePath == null -> "\nFailed to parse GraphQL stream $sourceLocation:\n$message"
        else -> {
          val document = try {
            File(filePath).readText()
          } catch (e: IOException) {
            throw RuntimeException("Failed to read GraphQL file `$this`", e)
          }
          val documentLines = document.lines()
          return "\nFailed to parse $filePath ${sourceLocation}\n$message" +
              "\n----------------------------------------------------\n" +
              kotlin.run {
                val prefix = if (sourceLocation.line - 2 >= 0) {
                  "[${sourceLocation.line - 1}]:" + documentLines[sourceLocation.line - 2]
                } else ""
                val body = if (sourceLocation.line - 1 >= 0) {
                  "\n[${sourceLocation.line}]:${documentLines[sourceLocation.line - 1]}\n"
                } else ""
                val postfix = if (sourceLocation.line < documentLines.size) {
                  "[${sourceLocation.line + 1}]:" + documentLines[sourceLocation.line]
                } else ""
                "$prefix$body$postfix"
              } +
              "\n----------------------------------------------------"
        }
      }
    }
  }
}

/**
 * Antlr found an error
 */
class AntlrParseException(message: String, val sourceLocation: SourceLocation) : SourceAwareException(message, sourceLocation)

/**
 * Something went wrong while building the GraphQL AST, analyzing the schema, ...
 *
 * This most likely means an Antlr rule was added to the grammar but the Kotlin code does not handle it
 */
class UnrecognizedAntlrRule(message: String, val sourceLocation: SourceLocation) : SourceAwareException(message, sourceLocation)

/**
 * An exception while converting to/from introspection
 *
 * This most likely means the json/sdl was inconsistent or corrupted
 */
class ConversionException(message: String, val sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : SourceAwareException(message, sourceLocation)

/**
 * The schema is invalid
 *
 * TODO: transform this into issues
 */
class SchemaValidationException(message: String, val sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : SourceAwareException(message, sourceLocation)