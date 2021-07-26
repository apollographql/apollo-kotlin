package com.apollographql.apollo3.ast

import java.io.File
import java.io.IOException

open class SourceAwareException(
    val error: String,
    val sourceLocation: SourceLocation,
) : RuntimeException(preview(
    error = error,
    sourceLocation = sourceLocation
)) {

  companion object {
    private fun formatForIdea(sourceLocation: SourceLocation, description: String): String {
      // Idea understands a certain format and makes logs clickable
      // It's not 100% clear where this is specified but it at least works with
      // 2020.3
      return "e: ${sourceLocation.pretty()}: $description"
    }

    private fun preview(error: String, sourceLocation: SourceLocation): String {
      val filePath = sourceLocation.filePath
      val preview = if (filePath != null) {
        val document = try {
          File(filePath).readText()
        } catch (e: IOException) {
          throw RuntimeException("Failed to read GraphQL file `$this`", e)
        }
        val documentLines = document.lines()
        "\n----------------------------------------------------\n" +
            run {
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
      } else {
        ""
      }
      return formatForIdea(sourceLocation, "$error$preview")
    }
  }
}

/**
 * Something went wrong while building the GraphQL AST, analyzing the schema, ...
 *
 * This most likely a bug. For an example, an Antlr rule was added to the grammar but the Kotlin code does not handle it
 */
class UnrecognizedAntlrRule(error: String, sourceLocation: SourceLocation) : SourceAwareException(error, sourceLocation)

/**
 * An exception while converting to/from introspection
 *
 * This most likely means the json/sdl was inconsistent or corrupted
 */
class ConversionException(error: String, sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : SourceAwareException(error, sourceLocation)

/**
 * The schema is invalid
 *
 * TODO: transform this into issues
 */
class SchemaValidationException(error: String, sourceLocation: SourceLocation = SourceLocation.UNKNOWN) : SourceAwareException(error, sourceLocation)
