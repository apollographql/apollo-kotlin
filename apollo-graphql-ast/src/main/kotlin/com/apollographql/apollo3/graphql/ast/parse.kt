/**
 * parse.kt
 *
 * Entry point for parsing GraphQL strings into a GraphQL AST based on [GQLNode](s)
 */
package com.apollographql.apollo3.graphql.ast

import com.apollographql.apollo3.compiler.parser.antlr.GraphQLLexer
import com.apollographql.apollo3.compiler.parser.antlr.GraphQLParser as AntlrGraphQLParser
import okio.BufferedSource
import okio.buffer
import okio.source
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * The result of a parsing operation.
 */
sealed class ParseResult<out V:Any> {
  class Success<V: Any>(val value: V) : ParseResult<V>()
  class Error(val issues: List<Issue.ParsingError>) : ParseResult<Nothing>()

  fun getOrNull() = (this as? Success)?.value
  fun getOrThrow(): V {
    when (this) {
      is Success -> return value
      is Error -> {
        check (issues.isNotEmpty())
        /**
         * All parsing errors are fatal
         */
        val firstError = issues.first()
        throw SourceAwareException(firstError.message, firstError.sourceLocation)
      }
    }
  }

  @OptIn(ExperimentalContracts::class)
  fun <R: Any> map(transform: (value: V) -> R): ParseResult<R> {
    contract {
      callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when(this) {
      is Success -> Success(transform(value))
      is Error -> Error(issues)
    }
  }
}

/**
 * Parses a GraphQL document to a [GQLDocument], validating the grammar but not the contents of the document.
 *
 * Use [toSchema] to parse and validate a [Schema] in one call.
 * Use [toExecutableGQLDefinitions] to parse and validate an executable document containing one or several
 * operation in one call.
 *
 * @return a [ParseResult] with either a non-null [GQLDocument] or a list of issues.
 */
fun BufferedSource.parseAsGQLDocument(filePath: String? = null): ParseResult<GQLDocument> = use {
  return antlrParse(it, filePath) { parser ->
    parser.document()
  }.map { documentContext ->
    documentContext.toGQLDocument(filePath)
  }
}

/**
 * See [parseAsGQLDocument]
 */
fun File.parseAsGQLDocument() = source().buffer().parseAsGQLDocument(absolutePath)

/**
 * See [parseAsGQLDocument]
 */
fun String.parseAsGQLDocument() = byteInputStream().source().buffer().parseAsGQLDocument()

/**
 * Parses the [BufferedSource] into a [GQLDocument]
 *
 * Throw if the document syntax is not correct but doesn't do additional validation
 *
 */
fun BufferedSource.toGQLDocument() = parseAsGQLDocument().getOrThrow()

/**
 * See [toGQLDocument]
 */
fun File.toGQLDocument() = parseAsGQLDocument().getOrThrow()

/**
 * See [toGQLDocument]
 */
fun String.toGQLDocument() = parseAsGQLDocument().getOrThrow()

/**
 * Parses a GraphQL value to a [GQLValue], validating the grammar but not the contents of the value.
 */
fun BufferedSource.parseAsGQLValue(filePath: String? = null): ParseResult<GQLValue> = use {
  return antlrParse(it, filePath) { parser ->
    parser.value()
  }.map { valueContext ->
    valueContext.toGQLValue()
  }
}

/**
 * See [parseAsGQLValue]
 */
fun File.parseAsGQLValue() = source().buffer().parseAsGQLValue(absolutePath)

/**
 * See [parseAsGQLValue]
 */
fun String.parseAsGQLValue() = byteInputStream().source().buffer().parseAsGQLValue()

/**
 * Plain parsing, without validation or adding the builtin types
 */
private fun <T : ParserRuleContext> antlrParse(
    source: BufferedSource,
    filePath: String? = null,
    startRule: (AntlrGraphQLParser) -> T,
): ParseResult<T> {
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

  val ruleContext = startRule(parser)
  val documentStopToken = ruleContext.getStop()
  val allTokens = (parser.tokenStream as CommonTokenStream).tokens

  /**
   * Check that there are no trailing tokens
   */
  if (documentStopToken != null && !allTokens.isNullOrEmpty()) {
    val lastToken = allTokens[allTokens.size - 1]
    val eof = lastToken.type == Token.EOF
    val sameChannel = lastToken.channel == documentStopToken.channel
    if (!eof && lastToken.tokenIndex > documentStopToken.tokenIndex && sameChannel) {
      issues.add(
          Issue.ParsingError(
              "Unsupported token (eof) `${lastToken.text}`",
              SourceLocation(lastToken.line, lastToken.charPositionInLine, filePath)
          )
      )
    }
  }

  return if (issues.isNotEmpty()) {
    ParseResult.Error(issues)
  } else {
    ParseResult.Success(ruleContext)
  }
}
