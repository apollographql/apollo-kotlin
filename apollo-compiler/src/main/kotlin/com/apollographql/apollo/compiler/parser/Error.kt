package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.ir.SourceLocation
import org.antlr.v4.runtime.Token
import java.io.File
import java.io.IOException

internal class ParseException(message: String, val sourceLocation: SourceLocation) : RuntimeException(message) {
  companion object {
    operator fun invoke(message: String, token: Token) = ParseException(
        message = message,
        sourceLocation = SourceLocation(token)
    )
  }
}

internal class GraphQLParseException(message: String) : RuntimeException(message) {
  override fun fillInStackTrace(): Throwable = this
}

internal class GraphQLDocumentParseException(
    message: String,
    sourceLocation: SourceLocation,
    graphQLFilePath: String
) : RuntimeException(preview(
    message = message,
    sourceLocation = sourceLocation,
    graphQLFilePath = graphQLFilePath
)) {

  override fun fillInStackTrace(): Throwable = this

  companion object {
    operator fun invoke(
        message: String,
        token: Token,
        graphQLFilePath: String
    ) = GraphQLDocumentParseException(
        message = message,
        sourceLocation = SourceLocation(token),
        graphQLFilePath = graphQLFilePath
    )

    operator fun invoke(
        parseException: ParseException,
        graphQLFilePath: String
    ) = GraphQLDocumentParseException(
        message = parseException.message!!,
        sourceLocation = parseException.sourceLocation,
        graphQLFilePath = graphQLFilePath
    )

    private fun preview(message: String, sourceLocation: SourceLocation, graphQLFilePath: String): String {
      val document = try {
        File(graphQLFilePath).readText()
      } catch (e: IOException) {
        throw RuntimeException("Failed to read GraphQL file `$this`", e)
      }

      if (sourceLocation == SourceLocation.UNKNOWN) {
        return "\nFailed to parse GraphQL file $graphQLFilePath:\n$message"
      }

      val documentLines = document.lines()
      return "\nFailed to parse GraphQL file $graphQLFilePath ${sourceLocation}\n$message" +
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
