package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLBooleanValue
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirective
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.GQLDirectiveLocation
import com.apollographql.apollo3.ast.GQLDocument
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLEnumTypeExtension
import com.apollographql.apollo3.ast.GQLEnumValue
import com.apollographql.apollo3.ast.GQLEnumValueDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFieldDefinition
import com.apollographql.apollo3.ast.GQLFloatValue
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeExtension
import com.apollographql.apollo3.ast.GQLInputValueDefinition
import com.apollographql.apollo3.ast.GQLIntValue
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeExtension
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLListValue
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLNullValue
import com.apollographql.apollo3.ast.GQLObjectField
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLObjectTypeExtension
import com.apollographql.apollo3.ast.GQLObjectValue
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLOperationTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeExtension
import com.apollographql.apollo3.ast.GQLSchemaDefinition
import com.apollographql.apollo3.ast.GQLSchemaExtension
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLStringValue
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.GQLUnionTypeExtension
import com.apollographql.apollo3.ast.GQLValue
import com.apollographql.apollo3.ast.GQLVariableDefinition
import com.apollographql.apollo3.ast.GQLVariableValue
import com.apollographql.apollo3.ast.SourceLocation
import okio.BufferedSource
import okio.Closeable

internal class Parser(source: BufferedSource, val filePath: String?): Closeable {
  private val lexer = Lexer(source)
  private var token = lexer.nextToken()
  private var lookaheadToken: Token? = null

  fun parseDocument(allowEmpty: Boolean): GQLDocument  {
    return GQLDocument(
        definitions = if (allowEmpty) {
          parseList<Token.StartOfFile, Token.EndOfFile, GQLDefinition>(::parseDefinition)
        } else {
          parseNonEmptyList<Token.StartOfFile, Token.EndOfFile, GQLDefinition>(::parseDefinition)
        },
        filePath
    )
  }

  fun parseValue(): GQLValue  {
    return parseTopLevel {
      parseValueInternal(false)
    }
  }

  fun parseSelections(): List<GQLSelection> {
   return parseList<Token.StartOfFile, Token.EndOfFile, GQLSelection>(::parseSelection)
  }

  fun parseType(): GQLType {
    return parseTopLevel(::parseTypeInternal)
  }

  override fun close() {
    lexer.close()
  }

  private fun advance() {
    if (lookaheadToken != null) {
      token = lookaheadToken!!
      lookaheadToken = null
    } else {
      token = lexer.nextToken()
    }
  }

  private inline fun <reified T : Token> expectToken(): T {
    val start = token
    if (start is T) {
      advance()
      return start
    } else {
      throw ParserException(
          "Expected ${T::class.simpleName}, found '$token'.",
          token,
      )
    }
  }

  private inline fun <reified T : Token> expectOptionalToken(): T? {
    val start = token
    if (start is T) {
      advance()
      return start
    }
    return null
  }

  private inline fun <reified OpenToken : Token, reified CloseToken : Token, T> parseNonEmptyList(
      crossinline parseOne: () -> T,
  ): List<T> {
    expectToken<OpenToken>()
    return buildList {
      do {
        add(parseOne())
      } while (expectOptionalToken<CloseToken>() == null)
    }
  }

  private inline fun <reified OpenToken : Token, reified CloseToken : Token, T> parseList(
      crossinline parseOne: () -> T,
  ): List<T> {
    expectToken<OpenToken>()
    return buildList {
      while (expectOptionalToken<CloseToken>() == null) {
        add(parseOne())
      }
    }
  }

  private inline fun <reified OpenToken : Token, reified CloseToken : Token, T> parseListOrNull(
      crossinline parseOne: () -> T,
  ): List<T> {
    if (!peek<OpenToken>()) {
      return emptyList()
    }
    return parseList<OpenToken, CloseToken, T>(parseOne)
  }

  private inline fun <reified OpenToken : Token, reified CloseToken : Token, T> parseNonEmptyListOrNull(
      crossinline parseOne: () -> T,
  ): List<T>? {
    if (!peek<OpenToken>()) {
      return null
    }
    return parseNonEmptyList<OpenToken, CloseToken, T>(parseOne)
  }

  private inline fun <reified DelimiterToken : Token, T> parseNonEmptyDelimitedList(
      crossinline parseOne: () -> T,
  ): List<T> {
    expectOptionalToken<DelimiterToken>()
    return buildList {
      do {
        add(parseOne())
      } while (expectOptionalToken<DelimiterToken>() != null)
    }
  }

  private fun parseSelectionSet(): List<GQLSelection> {
    return parseNonEmptyList<Token.LeftBrace, Token.RightBrace, GQLSelection>(::parseSelection)
  }

  private fun parseOptionalSelectionSet(): List<GQLSelection> {
    return parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLSelection>(::parseSelection).orEmpty()
  }

  private fun sourceLocation(): SourceLocation {
    return token.sourceLocation()
  }

  private fun parseSelection(): GQLSelection {
    return when (token) {
      is Token.Spread -> parseFragment()
      else -> parseField()
    }
  }

  private fun expectOptionalKeyword(keyword: String): Token.Name? {
    val start = token
    if (start is Token.Name && start.value == keyword) {
      advance()
      return start;
    }
    return null
  }

  private fun parseFragmentName(): String {
    if ((token as Token.Name).value == "on") {
      throw ParserException("'on' is not a valid fragment name", token)
    }
    return parseName()
  }

  private fun parseName(): String {
    return expectToken<Token.Name>().value
  }

  private inline fun <reified T : Token> peek(): Boolean {
    return token is T
  }

  private fun parseNamedType(): GQLNamedType {
    return GQLNamedType(
        sourceLocation = sourceLocation(),
        name = parseName()
    )
  }

  private fun parseFragment(): GQLSelection {
    val sourceLocation = sourceLocation()
    expectToken<Token.Spread>()

    val on = expectOptionalKeyword("on")
    return if (on == null && peek<Token.Name>()) {
      GQLFragmentSpread(
          sourceLocation = sourceLocation,
          name = parseFragmentName(),
          directives = parseDirectives(const = false)
      )
    } else {
      GQLInlineFragment(
          sourceLocation = sourceLocation,
          typeCondition = if (on != null) parseNamedType() else null,
          directives = parseDirectives(const = false),
          selections = parseSelectionSet()
      )
    }
  }

  private fun parseField(): GQLField {
    val sourceLocation = sourceLocation()
    val nameOrAlias = parseName()

    val alias: String?
    val name: String
    if (expectOptionalToken<Token.Colon>() != null) {
      alias = nameOrAlias
      name = parseName()
    } else {
      alias = null
      name = nameOrAlias
    }

    return GQLField(
        sourceLocation = sourceLocation,
        alias = alias,
        name = name,
        arguments = parseArguments(const = false),
        directives = parseDirectives(const = false),
        selections = parseOptionalSelectionSet()
    )
  }

  private fun Token.sourceLocation(): SourceLocation = SourceLocation(
      line, column, filePath
  )

  private fun lookaheadToken(): Token {
    if (token !is Token.EndOfFile) {
      if (lookaheadToken == null) {
        lookaheadToken = lexer.nextToken()
      }
      return lookaheadToken!!
    }

    return token
  }

  private fun parseOperationType(): String {
    return expectToken<Token.Name>().value
  }

  private fun parseOperationDefinition(): GQLOperationDefinition {
    val sourceLocation = sourceLocation()
    val description = expectOptionalToken<Token.String>()?.value

    if (peek<Token.LeftBrace>()) {
      return GQLOperationDefinition(
          sourceLocation = sourceLocation,
          operationType = "query",
          name = null,
          variableDefinitions = emptyList(),
          directives = emptyList(),
          selections = parseSelectionSet(),
          description = description
      )
    }

    val operationType = parseOperationType()

    val name: String?
    if (token is Token.Name) {
      name = parseName()
    } else {
      name = null
    }

    return GQLOperationDefinition(
        sourceLocation = sourceLocation,
        operationType = operationType,
        name = name,
        variableDefinitions = parseVariableDefinitions(),
        directives = parseDirectives(const = false),
        selections = parseSelectionSet(),
        description = description
    )
  }

  private fun parseVariableDefinitions(): List<GQLVariableDefinition> {
    return parseListOrNull<Token.LeftParenthesis, Token.RightParenthesis, GQLVariableDefinition> {
      parseVariableDefinition()
    }
  }

  private fun parseVariableDefinition(): GQLVariableDefinition {
    val sourceLocation = sourceLocation()
    expectToken<Token.Dollar>()
    val name = parseName()
    expectToken<Token.Colon>()
    val type = parseTypeInternal()
    val defaultValue = if (expectOptionalToken<Token.Equals>() != null) {
      parseValueInternal(const = true)
    } else {
      null
    }
    val directives = parseDirectives(const = true)
    return GQLVariableDefinition(
        sourceLocation = sourceLocation,
        name = name,
        type = type,
        defaultValue = defaultValue,
        directives = directives
    )
  }

  private fun parseDescription(): String? {
    return expectOptionalToken<Token.String>()?.value
  }

  private fun expectKeyword(keyword: String): String {
    val start = token
    if (start is Token.Name && start.value == keyword) {
      advance()
      return start.value
    }
    unexpected()
  }

  private fun parseOperationTypeDefinition(): GQLOperationTypeDefinition {
    val sourceLocation = sourceLocation()
    val operationType = parseOperationType()
    expectToken<Token.Colon>()
    val namedType = parseNamedType()

    return GQLOperationTypeDefinition(
        sourceLocation = sourceLocation,
        operationType = operationType,
        namedType = namedType.name
    )
  }

  private fun parseSchemaDefinition(): GQLSchemaDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()

    expectKeyword("schema")
    val directives = parseDirectives(const = true)
    val operationTypeDefinitions = parseNonEmptyList<Token.LeftBrace, Token.RightBrace, GQLOperationTypeDefinition> {
      parseOperationTypeDefinition()
    }

    return GQLSchemaDefinition(
        sourceLocation = sourceLocation,
        description = description,
        directives = directives,
        rootOperationTypeDefinitions = operationTypeDefinitions
    )
  }

  private fun parseSchemaExtension(): GQLSchemaExtension {
    val sourceLocation = sourceLocation()
    expectKeyword("extend")
    expectKeyword("schema")
    val directives = parseDirectives(const = true)
    val operationTypeDefinitions = parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLOperationTypeDefinition> {
      parseOperationTypeDefinition()
    }.orEmpty()

    if (directives.isEmpty() && operationTypeDefinitions.isEmpty()) {
      unexpected()
    }

    return GQLSchemaExtension(
        sourceLocation = sourceLocation,
        directives = directives,
        operationTypeDefinitions = operationTypeDefinitions
    )
  }

  private fun parseScalarTypeDefinition(): GQLScalarTypeDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("scalar")
    val name = parseName()
    val directives = parseDirectives(const = true)

    return GQLScalarTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives
    )
  }

  private fun parseScalarTypeExtension(): GQLScalarTypeExtension {
    val sourceLocation = sourceLocation()
    expectKeyword("extend")
    expectKeyword("scalar")
    val name = parseName()
    val directives = parseDirectives(const = true)

    if (directives.isEmpty()) {
      unexpected()
    }

    return GQLScalarTypeExtension(
        sourceLocation = sourceLocation,
        name = name,
        directives = directives
    )
  }


  private fun parseFieldDefinition(): GQLFieldDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    val name = parseName()
    val argumentDefinitions = parseArgumentDefinitions()
    expectToken<Token.Colon>()
    val type = parseTypeInternal()
    val directives = parseDirectives(const = true)

    return GQLFieldDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
        arguments = argumentDefinitions,
        type = type
    )
  }

  private fun parseFieldDefinitions(): List<GQLFieldDefinition> {
    return parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLFieldDefinition>(::parseFieldDefinition).orEmpty()
  }

  private fun parseObjectTypeDefinition(): GQLTypeDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    this.expectKeyword("type")
    val name = parseName()
    val interfaces = parseImplementsInterfaces()
    val directives = parseDirectives(const = true)
    val fields = parseFieldDefinitions()

    return GQLObjectTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields,
    )
  }

  private fun parseObjectTypeExtension(): GQLObjectTypeExtension {
    val sourceLocation = sourceLocation()
    this.expectKeyword("extend")
    this.expectKeyword("type")
    val name = parseName()
    val interfaces = parseImplementsInterfaces()
    val directives = parseDirectives(const = true)
    val fields = parseFieldDefinitions()

    if (interfaces.isEmpty() && directives.isEmpty() && fields.isEmpty()) {
      unexpected()
    }

    return GQLObjectTypeExtension(
        sourceLocation = sourceLocation,
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields,
    )
  }

  private fun parseInterfaceTypeDefinition(): GQLInterfaceTypeDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("interface")
    val name = parseName()
    val interfaces = parseImplementsInterfaces()
    val directives = parseDirectives(const = true)
    val fields = parseFieldDefinitions()

    return GQLInterfaceTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields
    )
  }

  private fun parseInterfaceTypeExtension(): GQLInterfaceTypeExtension {
    val sourceLocation = sourceLocation()
    expectKeyword("extend")
    expectKeyword("interface")
    val name = parseName()
    val interfaces = parseImplementsInterfaces()
    val directives = parseDirectives(const = true)
    val fields = parseFieldDefinitions()

    if (interfaces.isEmpty() && directives.isEmpty() && fields.isEmpty()) {
      unexpected()
    }

    return GQLInterfaceTypeExtension(
        sourceLocation = sourceLocation,
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields
    )
  }

  private fun parseUnionTypeDefinition(): GQLUnionTypeDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("union")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val memberTypes = parseUnionMemberTypes()

    return GQLUnionTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
        memberTypes = memberTypes
    )
  }

  private fun parseUnionTypeExtension(): GQLUnionTypeExtension {
    val sourceLocation = sourceLocation()
    val start = token
    expectKeyword("extend")
    expectKeyword("union")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val memberTypes = parseUnionMemberTypes()

    if (directives.isEmpty() && memberTypes.isEmpty()) {
      unexpected(start)
    }

    return GQLUnionTypeExtension(
        sourceLocation = sourceLocation,
        name = name,
        directives = directives,
        memberTypes = memberTypes
    )
  }

  private fun parseUnionMemberTypes(): List<GQLNamedType> {
    return if (expectOptionalToken<Token.Equals>() != null) {
      parseNonEmptyDelimitedList<Token.Pipe, GQLNamedType>(::parseNamedType)
    } else {
      emptyList()
    }
  }

  private fun parseInputObjectTypeDefinition(): GQLInputObjectTypeDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("input")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val inputFields = parseInputFieldDefinitions()

    return GQLInputObjectTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
        inputFields = inputFields,
    )
  }

  private fun parseInputObjectTypeExtension(): GQLInputObjectTypeExtension {
    val sourceLocation = sourceLocation()
    expectKeyword("extend")
    expectKeyword("input")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val inputFields = parseInputFieldDefinitions()

    if (directives.isEmpty() && inputFields.isEmpty()) {
      unexpected()
    }

    return GQLInputObjectTypeExtension(
        sourceLocation = sourceLocation,
        name = name,
        directives = directives,
        inputFields = inputFields,
    )
  }

  private fun parseInputFieldDefinitions(): List<GQLInputValueDefinition> {
    return parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLInputValueDefinition>(::parseInputValueDefinition).orEmpty()
  }

  private fun parseEnumTypeDefinition(): GQLEnumTypeDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("enum")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val values = parseEnumValueDefinitions()

    return GQLEnumTypeDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives,
        enumValues = values
    )
  }

  private fun parseEnumTypeExtension(): GQLEnumTypeExtension {
    val sourceLocation = sourceLocation()
    expectKeyword("extend")
    expectKeyword("enum")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val values = parseEnumValueDefinitions()

    if (directives.isEmpty() && values.isEmpty()) {
      unexpected()
    }

    return GQLEnumTypeExtension(
        sourceLocation = sourceLocation,
        name = name,
        directives = directives,
        enumValues = values
    )
  }

  private fun parseEnumValueDefinitions(): List<GQLEnumValueDefinition> {
    return parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLEnumValueDefinition>(::parseEnumValueDefinition).orEmpty()
  }

  private fun parseEnumValueDefinition(): GQLEnumValueDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    val name = parseEnumValueName()
    val directives = parseDirectives(const = true)

    return GQLEnumValueDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        directives = directives
    )
  }

  private fun parseEnumValueName(): String {
    val start = token
    val name = expectToken<Token.Name>().value

    when (name) {
      "true",
      "false",
      "null",
      -> {
        throw ParserException("'$name' is reserved and cannot be used for an enum value", start)
      }
    }
    return name
  }

  private fun parseDirectiveDefinition(): GQLDirectiveDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("directive")
    expectToken<Token.At>()
    val name = parseName()
    val arguments = parseArgumentDefinitions()
    val repeatable = expectOptionalKeyword("repeatable") != null
    expectKeyword("on")
    val locations = parseDirectiveLocations()

    return GQLDirectiveDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        arguments = arguments,
        repeatable = repeatable,
        locations = locations
    )
  }

  private fun parseTypeSystemExtension(): GQLDefinition {
    val t = lookaheadToken()

    if (t !is Token.Name) {
      unexpected(t)
    }

    return when (t.value) {
      "schema" -> parseSchemaExtension()
      "scalar" -> parseScalarTypeExtension()
      "type" -> parseObjectTypeExtension()
      "interface" -> parseInterfaceTypeExtension()
      "union" -> parseUnionTypeExtension()
      "enum" -> parseEnumTypeExtension()
      "input" -> parseInputObjectTypeExtension()
      else -> unexpected(t)
    }
  }

  private fun parseDefinition(): GQLDefinition {

    val hasDescription = peek<Token.String>()
    val t = if (hasDescription) lookaheadToken() else token

    return when (t) {
      is Token.LeftBrace -> parseOperationDefinition()
      is Token.Name -> {
        if (t.value == "extend" && hasDescription) {
          throw ParserException("Type system extensions cannot have a description", t)
        }
        when (t.value) {
          "schema" -> parseSchemaDefinition()
          "scalar" -> parseScalarTypeDefinition()
          "type" -> parseObjectTypeDefinition()
          "interface" -> parseInterfaceTypeDefinition()
          "union" -> parseUnionTypeDefinition()
          "enum" -> parseEnumTypeDefinition()
          "input" -> parseInputObjectTypeDefinition()
          "directive" -> parseDirectiveDefinition()
          "query", "mutation", "subscription" -> parseOperationDefinition()
          "fragment" -> parseFragmentDefinition()
          "extend" -> parseTypeSystemExtension()
          else -> unexpected(t)
        }
      }

      else -> unexpected()
    }
  }

  private fun parseFragmentDefinition(): GQLFragmentDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    expectKeyword("fragment")
    val name = parseFragmentName()
    expectKeyword("on")
    val typeCondition = parseNamedType()
    val directives = parseDirectives(const = false)
    val selections = parseSelectionSet()
    return GQLFragmentDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        typeCondition = typeCondition,
        directives = directives,
        selections = selections
    )
  }

  private fun parseArgumentDefinitions(): List<GQLInputValueDefinition> {
    return parseNonEmptyListOrNull<Token.LeftParenthesis, Token.RightParenthesis, GQLInputValueDefinition>(::parseInputValueDefinition).orEmpty()
  }

  private fun parseInputValueDefinition(): GQLInputValueDefinition {
    val sourceLocation = sourceLocation()
    val description = parseDescription()
    val name = parseName()
    expectToken<Token.Colon>()
    val type = parseTypeInternal()
    val defaultValue = if (expectOptionalToken<Token.Equals>() != null) {
      parseValueInternal(const = true)
    } else {
      null
    }
    val directives = parseDirectives(const = true)

    return GQLInputValueDefinition(
        sourceLocation = sourceLocation,
        description = description,
        name = name,
        type = type,
        directives = directives,
        defaultValue = defaultValue
    )
  }

  private fun parseImplementsInterfaces(): List<String> {
    if (expectOptionalKeyword("implements") == null) {
      return emptyList()
    }

    return parseNonEmptyDelimitedList<Token.Ampersand, String>(::parseName)
  }

  private fun parseDirectiveLocations(): List<GQLDirectiveLocation> {
    return this.parseNonEmptyDelimitedList<Token.Pipe, GQLDirectiveLocation>(::parseDirectiveLocation)
  }

  private fun parseDirectiveLocation(): GQLDirectiveLocation {
    val start = token
    return try {
      GQLDirectiveLocation.valueOf(parseName())
    } catch (e: IllegalArgumentException) {
      unexpected(start)
    }
  }

  private fun parseDirectives(const: Boolean): List<GQLDirective> {
    return buildList {
      while (token is Token.At) {
        add(parseDirective(const))
      }
    }
  }

  private fun parseDirective(const: Boolean): GQLDirective {
    val sourceLocation = sourceLocation()

    expectToken<Token.At>()
    val name = parseName()
    val args = parseArguments(const)
    return GQLDirective(sourceLocation, name, args)
  }

  private fun parseArguments(const: Boolean): List<GQLArgument> {
    return parseNonEmptyListOrNull<Token.LeftParenthesis, Token.RightParenthesis, GQLArgument> { parseArgument(const) }.orEmpty()
  }

  private fun parseArgument(const: Boolean): GQLArgument {
    val sourceLocation = sourceLocation()
    val name = parseName()

    expectToken<Token.Colon>()
    return GQLArgument(
        sourceLocation = sourceLocation,
        name = name,
        value = parseValueInternal(const)
    )
  }

  private fun unexpected(token: Token? = null): Nothing {
    val t = token ?: this.token
    throw ParserException("Unexpected token: '$t'", t)
  }

  private fun <T> parseTopLevel(block: () -> T): T {
    expectToken<Token.StartOfFile>()
    return block().also {
      expectToken<Token.EndOfFile>()
    }
  }


  private fun parseValueInternal(const: Boolean): GQLValue {
    val sourceLocation = sourceLocation()
    return when (val t = token) {
      is Token.LeftBracket -> parseList(const)
      is Token.LeftBrace -> parseObject(const)

      is Token.Int -> GQLIntValue(sourceLocation, t.value).also { advance() }
      is Token.Float -> GQLFloatValue(sourceLocation, t.value).also { advance() }
      is Token.String -> GQLStringValue(sourceLocation, t.value).also { advance() }
      is Token.Name -> when (t.value) {
        "true" -> GQLBooleanValue(sourceLocation, true).also { advance() }
        "false" -> GQLBooleanValue(sourceLocation, false).also { advance() }
        "null" -> GQLNullValue(sourceLocation).also { advance() }
        else -> GQLEnumValue(sourceLocation, t.value).also { advance() }
      }

      is Token.Dollar -> {
        advance()
        if (const) {
          val n = expectOptionalToken<Token.Name>()
          if (n != null) {
            throw ParserException("Unexpected variable '${n.value}' in constant value.", n)
          } else {
            unexpected(n)
          }
        }
        return GQLVariableValue(
            sourceLocation = sourceLocation,
            name = parseName()
        )
      }

      else -> unexpected()
    }
  }

  private fun parseObject(const: Boolean): GQLObjectValue {
    return GQLObjectValue(
        sourceLocation = sourceLocation(),
        fields = parseList<Token.LeftBrace, Token.RightBrace, GQLObjectField> {
          parseObjectField(const)
        }
    )
  }

  private fun parseObjectField(const: Boolean): GQLObjectField {
    val sourceLocation = sourceLocation()
    val name = parseName()
    expectToken<Token.Colon>()
    val value = parseValueInternal(const)
    return GQLObjectField(
        sourceLocation = sourceLocation,
        name = name,
        value = value
    )
  }

  private fun parseList(const: Boolean): GQLListValue {
    return GQLListValue(
        sourceLocation = sourceLocation(),
        values = parseList<Token.LeftBracket, Token.RightBracket, GQLValue> {
          parseValueInternal(const)
        }
    )
  }

  private fun parseTypeInternal(): GQLType {
    val sourceLocation = sourceLocation()

    val type = if (expectOptionalToken<Token.LeftBracket>() != null) {
      val ofType = parseTypeInternal()
      expectToken<Token.RightBracket>()

      GQLListType(
          sourceLocation = sourceLocation,
          type = ofType
      )
    } else {
      parseNamedType()
    }

    return if (token is Token.ExclamationPoint) {
      advance()
      GQLNonNullType(
          sourceLocation = sourceLocation,
          type
      )
    } else {
      type
    }
  }
}
