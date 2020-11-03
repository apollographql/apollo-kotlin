package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.parser.antlr.GraphQLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.File
import java.io.InputStream

fun GQLDocument.withBuiltinTypes(): GQLDocument {
  val buildInsInputStream = javaClass.getResourceAsStream("/builtins.sdl")
  return copy(
      definitions = definitions + GQLDocument.parseInternal(buildInsInputStream).definitions
  )
}

/**
 * Plain parsing, without validation or adding the builtin types
 */
private fun GQLDocument.Companion.parseInternal(inputStream: InputStream, filePath: String = "(source)"): GQLDocument {

  return GraphQLParser(
      CommonTokenStream(
          GraphQLLexer(
              CharStreams.fromStream(inputStream)
          )
      )
  ).apply {
    removeErrorListeners()
    interpreter.predictionMode = PredictionMode.SLL
    addErrorListener(
        object : BaseErrorListener() {
          override fun syntaxError(
              recognizer: Recognizer<*, *>?,
              offendingSymbol: Any?,
              line: Int,
              position: Int,
              msg: String?,
              e: RecognitionException?
          ) {
            throw ParseException(
                message = "Unsupported token `${(offendingSymbol as? Token)?.text ?: offendingSymbol.toString()}`",
                sourceLocation = com.apollographql.apollo.compiler.ir.SourceLocation(
                    line = line,
                    position = position
                )
            )
          }
        }
    )
  }.document()
      .parse()
}

fun GQLDocument.Companion.fromString(document: String) = GQLDocument.fromInputStream(document.byteInputStream())

fun GQLDocument.Companion.fromFile(file: File) = file.inputStream().use {
  GQLDocument.fromInputStream(it, file.absolutePath)
}

fun GQLDocument.Companion.fromInputStream(inputStream: InputStream, filePath: String = "(source)"): GQLDocument {
  // Validation as to be done before adding the built in types else validation fail on names starting with '__'
  // This means that it's impossible to add type extension on built in types at the moment
  return try {
    parseInternal(inputStream, filePath)
        .mergeTypeExtensions()
        .validateAsSchema()
        .withBuiltinTypes()
  } catch (e: ParseException) {
    throw DocumentParseException(
        parseException = e,
        filePath = filePath
    )
  }
}