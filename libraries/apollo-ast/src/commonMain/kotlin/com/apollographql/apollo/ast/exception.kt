package com.apollographql.apollo.ast

import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer


open class SourceAwareException(
    val error: String,
    val sourceLocation: SourceLocation?,
) : RuntimeException(preview(
    error = error,
    sourceLocation = sourceLocation
)) {

  companion object {
    private fun formatForIdea(sourceLocation: SourceLocation?, description: String): String {
      // Idea understands a certain format and makes logs clickable
      // It's not 100% clear where this is specified but it at least works with
      // 2020.3
      return "e: ${sourceLocation.pretty()}: $description"
    }

    private fun preview(error: String, sourceLocation: SourceLocation?): String {
      val preview = if (sourceLocation?.filePath != null && sourceLocation.line >= 1 && sourceLocation.column >= 1) {
        val filePath = sourceLocation.filePath
        val document = try {
          HOST_FILESYSTEM.source(filePath.toPath()).buffer().readUtf8()
        } catch (e: IOException) {
          throw RuntimeException("Failed to read GraphQL file `$this`", e)
        }
        val documentLines = document.lines()
        "\n----------------------------------------------------\n" +
            run {
              val prefix = if (sourceLocation.line - 2 >= 0) {
                "[${sourceLocation.line - 1}]:" + documentLines[sourceLocation.line - 2]
              } else ""
              val body = "\n[${sourceLocation.line}]:${documentLines[sourceLocation.line - 1]}\n"
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
 * An exception while converting to/from introspection
 *
 * This most likely means the json/sdl was inconsistent or corrupted
 */
class ConversionException(error: String, sourceLocation: SourceLocation? = null) : SourceAwareException(error, sourceLocation)

/**
 * The schema is invalid
 *
 * TODO: transform this into issues
 */
class SchemaValidationException(error: String, sourceLocation: SourceLocation? = null) : SourceAwareException(error, sourceLocation)
