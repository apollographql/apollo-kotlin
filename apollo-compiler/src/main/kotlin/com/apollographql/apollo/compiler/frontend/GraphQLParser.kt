package com.apollographql.apollo.compiler.frontend

import com.apollographql.apollo.compiler.parser.antlr.GraphQLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser as AntlrGraphQLParser
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

/**
 * Entry point for parsing and validating GraphQL objects
 */
object GraphQLParser {
  /**
   * Parses and validates the given SDL schema document
   *
   * This voluntarily returns a [Schema]  and not a [GQLDocument] in order to explicitly distinguish between operations and schemas
   * throws if the schema has errors or is not a schema file
   */
  fun parseSchema(source: BufferedSource, filePath: String? = null): Schema {
    return parseSchemaInternal(source, filePath)
        .mapValue {
          it.toSchema()
        }.orThrow()
  }

  /**
   * Parses and validates the given SDL schema document
   *
   * This voluntarily returns a [Schema]  and not a [GQLDocument] in order to explicitly distinguish between operations and schemas
   * throws if the schema has errors or is not a schema file
   */
  fun parseSchema(string: String) = parseSchema(string.byteInputStream().source().buffer())

  /**
   * Parses and validates the given SDL schema document
   *
   * This voluntarily does not return a GQLDocument in order to explicitly distinguish between operations and schemas
   * throws if the schema has errors or is not a schema file
   */
  fun parseSchema(file: File) = parseSchema(file.source().buffer(), file.absolutePath)

  /**
   * Parses and validates the given SDL executable document, containing operations and/or fragments
   */
  fun parseOperations(source: BufferedSource, filePath: String? = null, schema: Schema): ParseResult<GQLDocument> {
    return parseDocument(source, filePath).appendIssues {
      it.validateAsOperations(schema)
    }
  }

  /**
   * Parse the given SDL document
   */
  fun parseOperations(string: String, schema: Schema) = parseOperations(string.byteInputStream().source().buffer(), null, schema)

  /**
   * Parse the given SDL document
   */
  fun parseOperations(file: File, schema: Schema) = file.source().buffer().use {
    parseOperations(it, file.absolutePath, schema)
  }

  /**
   * A specialized version that works on multiple files and can also inject fragments from another
   * compilation unit
   */
  fun parseExecutableFiles(files: Set<File>, schema: Schema, injectedFragmentDefinitions: List<GQLFragmentDefinition>): Pair<List<GQLDocument>, List<Issue>> {
    val (documents, parsingIssues) = files.map { parseDocument(it) }
        .fold(Pair<List<GQLDocument>, List<Issue>>(emptyList(), emptyList())) { acc, item ->
          Pair(
              acc.first + (item.value?.let { listOf(it) } ?: emptyList()),
              acc.second + item.issues
          )
        }

    val allDefinitions = documents.flatMap { it.definitions } + injectedFragmentDefinitions

    val duplicateIssues = allDefinitions.checkDuplicates()

    /**
     * Collect all fragments as operations might use fragments from different files
     */
    val allFragments = allDefinitions.filterIsInstance<GQLFragmentDefinition>().associateBy {
      it.name
    }

    val validationIssues = documents.flatMap { document ->
      document.definitions.flatMap { definition ->
        when (definition) {
          is GQLFragmentDefinition -> {
            // This will catch unused fragments that are invalid. It might not be strictly needed
            definition.validate(schema, allFragments)
          }
          is GQLOperationDefinition -> {
            definition.validate(schema, allFragments)
          }
          else -> {
            listOf(
                Issue.ValidationError(
                    message = "Non-executable definition found",
                    sourceLocation = definition.sourceLocation,
                )
            )
          }
        }
      }
    }

    return documents to (parsingIssues + duplicateIssues + validationIssues)
  }

  /**
   * Parses a GraphQL document without doing any kind of validation besides the grammar parsing.
   *
   * Use [parseOperations] to parse operations and [parseSchema] to have proper validation
   *
   * @return a [ParseResult] with either a non-null [GQLDocument] or a list of issues
   */
  fun parseDocument(source: BufferedSource, filePath: String?): ParseResult<GQLDocument> = source.use { _ ->
    val parseResult = antlrParse(source, filePath) {
      it.document()
    }

    if (parseResult.issues.isNotEmpty()) {
      // If there are issues, return now. We cannot parse a document with errors.
      ParseResult(null, parseResult.issues)
    } else {
      ParseResult(parseResult.value!!.toGQLDocument(filePath), parseResult.issues)
    }
  }

  fun parseDocument(file: File) = parseDocument(file.source().buffer(), file.absolutePath)

  fun parseDocument(string: String) = parseDocument(string.byteInputStream().source().buffer(), null)

  fun parseValue(string: String): ParseResult<GQLValue> {
    return antlrParse(string.byteInputStream().source().buffer(), null) {
      it.value()
    }.mapValue {
      it.toGQLValue()
    }
  }

  fun builtinTypes(): GQLDocument {
    val source = GQLDocument::class.java.getResourceAsStream("/builtins.sdl")
        .source()
        .buffer()
    return antlrParse(source, null) { it.document() }
        .orThrow()
        .toGQLDocument(null)
  }

  internal fun parseSchemaInternal(source: BufferedSource, filePath: String? = null): ParseResult<GQLDocument> {
    return parseDocument(source, filePath)
        .flatMap {
          it.mergeTypeExtensions()
        }
        .appendIssues {
          // Validation as to be done before adding the built in types else validation fail on names starting with '__'
          // This means that it's impossible to add type extensions on built in types at the moment
          it.validateAsSchema()
        }.mapValue {
          it.withBuiltinTypes()
        }
  }

  internal fun parseSchemaInternal(string: String) = parseSchemaInternal(string.byteInputStream().source().buffer())
  internal fun parseSchemaInternal(file: File) = parseSchemaInternal(file.source().buffer(), file.absolutePath)

  /**
   * Plain parsing, without validation or adding the builtin types
   */
  private fun <T : ParserRuleContext> antlrParse(
      source: BufferedSource,
      filePath: String? = null,
      startRule: (AntlrGraphQLParser) -> T
  ): ParseResult<T> {
    val parser = AntlrGraphQLParser(
        CommonTokenStream(
            GraphQLLexer(
                CharStreams.fromStream(source.inputStream())
            )
        )
    )

    val issues = mutableListOf<Issue>()
    return parser.apply {
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
              issues.add(Issue.ParsingError(
                  message = "Unsupported token `${(offendingSymbol as? Token)?.text ?: offendingSymbol.toString()}`",
                  sourceLocation = SourceLocation(line, position, filePath)
              ))
            }
          }
      )
    }.let { startRule(it) }
        .also {
          parser.checkEOF(it, filePath)
        }
        .let {
          ParseResult(it, issues)
        }
  }

  private fun AntlrGraphQLParser.checkEOF(parserRuleContext: ParserRuleContext, filePath: String?) {
    val documentStopToken = parserRuleContext.getStop()
    val allTokens = (tokenStream as CommonTokenStream).tokens
    if (documentStopToken != null && !allTokens.isNullOrEmpty()) {
      val lastToken = allTokens[allTokens.size - 1]
      val eof = lastToken.type == Token.EOF
      val sameChannel = lastToken.channel == documentStopToken.channel
      if (!eof && lastToken.tokenIndex > documentStopToken.tokenIndex && sameChannel) {
        throw AntlrParseException(
            message = "Unsupported token `${lastToken.text}`",
            sourceLocation = SourceLocation(lastToken.line, lastToken.charPositionInLine, filePath)
        )
      }
    }
  }
}
