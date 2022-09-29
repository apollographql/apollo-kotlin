/**
 * antlrParse.kt
 *
 * Entry point for parsing GraphQL strings into a GraphQL AST based on [GQLNode](s)
 */
package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.GQLResult
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.containsError
import com.apollographql.apollo3.generated.antlr.GraphQLLexer
import okio.BufferedSource
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import com.apollographql.apollo3.generated.antlr.GraphQLParser as AntlrGraphQLParser

/**
 * Sets up a parser
 *
 * @param startRule returns the start rule and consumes the stream. It is expected that startRule
 * consumes the whole source
 * @param convert converts startRule to something else, most of the times a GQLNode. Not called if there are
 * any parsing issues
 */
internal fun <T : RuleContext, R: Any> antlrParse(
    source: BufferedSource,
    filePath: String? = null,
    startRule: (AntlrGraphQLParser) -> T,
    convert: (T) -> R
): GQLResult<R> {
  val parser = AntlrGraphQLParser(
      CommonTokenStream(
          GraphQLLexer(
              CharStreams.fromStream(source.inputStream())
          )
      )
  )

  val issues = mutableListOf<Issue.ParsingError>()

  with(parser) {
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
              e: RecognitionException?,
          ) {
            issues.add(Issue.ParsingError(
                message = "Unsupported token `${(offendingSymbol as? Token)?.text ?: offendingSymbol.toString()}`",
                sourceLocation = SourceLocation(line, position, filePath)
            ))
          }
        }
    )
  }

  val result = startRule(parser)

  val currentToken = parser.currentToken
  if (currentToken.type != Token.EOF) {
    issues.add(
        Issue.ParsingError(
            "Extra token at end of file `${currentToken.text}`",
            SourceLocation(currentToken.line, currentToken.charPositionInLine, filePath)
        )
    )
  }

  val value = if (issues.containsError()) {
    // If there is any parsing error, we can't convert to our internal AST
    null
  } else {
    convert(result)
  }
  return GQLResult(value, issues)
}
