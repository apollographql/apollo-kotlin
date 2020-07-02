package com.apollographql.apollo.compiler.parser.error

import com.apollographql.apollo.compiler.ir.SourceLocation
import org.antlr.v4.runtime.Token
import java.io.File
import java.io.IOException

internal class DocumentParseException(
    message: String,
    sourceLocation: SourceLocation,
    filePath: String
) : RuntimeException(preview(
    message = message,
    sourceLocation = sourceLocation,
    filePath = filePath
)) {

  override fun fillInStackTrace(): Throwable = this

  companion object {
    operator fun invoke(
        message: String,
        token: Token,
        filePath: String
    ) = DocumentParseException(
        message = message,
        sourceLocation = SourceLocation(
            line = token.line,
            position = token.charPositionInLine
        ),
        filePath = filePath
    )

    operator fun invoke(
        parseException: ParseException,
        filePath: String
    ) = DocumentParseException(
        message = parseException.message!!,
        sourceLocation = parseException.sourceLocation,
        filePath = filePath
    )

    private fun preview(message: String, sourceLocation: SourceLocation, filePath: String): String {
      val document = try {
        File(filePath).readText()
      } catch (e: IOException) {
        throw RuntimeException("Failed to read GraphQL file `$this`", e)
      }

      if (sourceLocation == SourceLocation.UNKNOWN) {
        return "\nFailed to parse GraphQL file $filePath:\n$message"
      }

      val documentLines = document.lines()
      return "\nFailed to parse GraphQL file $filePath ${sourceLocation}\n$message" +
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
