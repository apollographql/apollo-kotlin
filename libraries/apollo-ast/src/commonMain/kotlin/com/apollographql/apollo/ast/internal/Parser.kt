package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.ast.*

internal class Parser(
    src: String,
    options: ParserOptions,
    private val filePath: String?,
) {
  private val lexer = Lexer(src)
  private var token = lexer.nextToken()
  private var lastToken = token
  private var lookaheadToken: Token? = null
  private val withSourceLocation = options.withSourceLocation
  private val allowEmptyDocuments = options.allowEmptyDocuments
  private val allowDirectivesOnDirectives = options.allowDirectivesOnDirectives
  private val allowServiceCapabilities = options.allowServiceCapabilities
  private val allowFragmentArguments = options.allowFragmentArguments

  fun parseDocument(): GQLDocument {
    val start = token
    val definitions = if (allowEmptyDocuments) {
      parseList<Token.StartOfFile, Token.EndOfFile, GQLDefinition>(::parseDefinition)
    } else {
      parseNonEmptyList<Token.StartOfFile, Token.EndOfFile, GQLDefinition>(::parseDefinition)
    }
    return GQLDocument(
        definitions = definitions,
        sourceLocation = sourceLocation(start)
    )
  }

  fun parseValue(): GQLValue {
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

  fun parseSchemaCoordinate(): GQLSchemaCoordinate {
    return parseTopLevel(::parseSchemaCoordinateInternal)
  }

  private fun advance() {
    lastToken = token
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
      return start
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

  private fun parseQualifiedName(): String {
    val components = mutableListOf<String>()
    components.add(parseName())
    expectToken<Token.Dot>()
    components.add(parseName())
    while (peek<Token.Dot>()) {
      advance()
      components.add(parseName())
    }
    return components.joinToString(".")
  }

  private inline fun <reified T : Token> peek(): Boolean {
    return token is T
  }

  private fun parseNamedType(): GQLNamedType {
    val start = token
    val name = parseName()
    val sourceLocation = sourceLocation(start)
    return GQLNamedType(
        sourceLocation = sourceLocation,
        name = name
    )
  }

  private fun parseFragment(): GQLSelection {
    val start = token
    expectToken<Token.Spread>()

    val on = expectOptionalKeyword("on")
    return if (on == null && peek<Token.Name>()) {
      val name = parseFragmentName()
      val arguments: List<GQLArgument>
      if (allowFragmentArguments) {
        arguments = parseArguments(false)
      } else {
        arguments = emptyList()
      }
      val directives = parseDirectives(const = false)
      GQLFragmentSpread(
          sourceLocation = sourceLocation(start),
          name = name,
          arguments = arguments,
          directives = directives,
      )
    } else {
      val typeCondition = if (on != null) parseNamedType() else null
      val directives = parseDirectives(const = false)
      val selections = parseSelectionSet()
      GQLInlineFragment(
          sourceLocation = sourceLocation(start),
          typeCondition = typeCondition,
          directives = directives,
          selections = selections
      )
    }
  }

  private fun parseField(): GQLField {
    val start = token
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

    val arguments = parseArguments(const = false)
    val directives = parseDirectives(const = false)

    val selections = parseOptionalSelectionSet()
    return GQLField(
        sourceLocation = sourceLocation(start),
        alias = alias,
        name = name,
        arguments = arguments,
        directives = directives,
        selections = selections,
    )
  }

  private fun sourceLocation(from: Token): SourceLocation? {
    if (!withSourceLocation) return null

    return SourceLocation(
        start = from.start,
        end = lastToken.end,
        line = from.line,
        column = from.column,
        filePath = filePath
    )
  }

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

  private fun parseOperationDefinition(start: Token): GQLOperationDefinition {
    val description = expectOptionalToken<Token.String>()?.value

    if (peek<Token.LeftBrace>()) {
      val selections = parseSelectionSet()
      return GQLOperationDefinition(
          sourceLocation = sourceLocation(start),
          operationType = "query",
          name = null,
          variableDefinitions = emptyList(),
          directives = emptyList(),
          selections = selections,
          description = description
      )
    }

    val operationType = parseOperationType()

    val name = if (token is Token.Name) {
      parseName()
    } else {
      null
    }

    val variableDefinitions = parseVariableDefinitions()
    val directives = parseDirectives(const = false)
    val selections = parseSelectionSet()
    return GQLOperationDefinition(
        sourceLocation = sourceLocation(start),
        operationType = operationType,
        name = name,
        variableDefinitions = variableDefinitions,
        directives = directives,
        selections = selections,
        description = description
    )
  }

  private fun parseVariableDefinitions(): List<GQLVariableDefinition> {
    return parseListOrNull<Token.LeftParenthesis, Token.RightParenthesis, GQLVariableDefinition> {
      parseVariableDefinition()
    }
  }

  private fun parseVariableDefinition(): GQLVariableDefinition {
    val description = expectOptionalToken<Token.String>()?.value
    val start = token
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
        sourceLocation = sourceLocation(start),
        name = name,
        type = type,
        defaultValue = defaultValue,
        directives = directives,
        description = description,
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

  private fun parseServiceCapability(): GQLCapability {
    val start = token
    val description = parseDescription()

    expectKeyword("capability")

    val qualifiedName = parseQualifiedName()

    var value: String? = null
    if (expectOptionalToken<Token.LeftParenthesis>() != null) {
      value = expectToken<Token.String>().value
      expectToken<Token.RightParenthesis>()
    }
    return GQLCapability(
        sourceLocation = sourceLocation(start),
        description = description,
        name = qualifiedName,
        value = value
    )
  }

  private fun parseOperationTypeDefinition(): GQLOperationTypeDefinition {
    val start = token
    val operationType = parseOperationType()
    expectToken<Token.Colon>()
    val namedType = parseNamedType()

    return GQLOperationTypeDefinition(
        sourceLocation = sourceLocation(start),
        operationType = operationType,
        namedType = namedType.name
    )
  }

  private fun parseSchemaDefinition(start: Token): GQLSchemaDefinition {
    val description = parseDescription()

    expectKeyword("schema")
    val directives = parseDirectives(const = true)
    val operationTypeDefinitions = parseNonEmptyList<Token.LeftBrace, Token.RightBrace, GQLOperationTypeDefinition> {
      parseOperationTypeDefinition()
    }

    return GQLSchemaDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        directives = directives,
        rootOperationTypeDefinitions = operationTypeDefinitions
    )
  }

  private fun parseServiceDefinition(start: Token): GQLServiceDefinition {
    val description = parseDescription()

    expectKeyword("service")
    val directives = parseDirectives(const = true)
    val capabilities = parseList<Token.LeftBrace, Token.RightBrace, GQLCapability> {
      parseServiceCapability()
    }

    return GQLServiceDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        directives = directives,
        capabilities = capabilities
    )
  }

  private fun parseSchemaExtension(): GQLSchemaExtension {
    val start = token
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
        sourceLocation = sourceLocation(start),
        directives = directives,
        operationTypeDefinitions = operationTypeDefinitions
    )
  }

  private fun parseServiceExtension(): GQLServiceExtension {
    val start = token
    expectKeyword("extend")
    expectKeyword("service")
    val directives = parseDirectives(const = true)
    val capabilities = parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLCapability> {
      parseServiceCapability()
    }.orEmpty()

    return GQLServiceExtension(
        sourceLocation = sourceLocation(start),
        directives = directives,
        capabilities = capabilities
    )
  }

  private fun parseScalarTypeDefinition(start: Token): GQLScalarTypeDefinition {
    val description = parseDescription()
    expectKeyword("scalar")
    val name = parseName()
    val directives = parseDirectives(const = true)

    return GQLScalarTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        directives = directives
    )
  }

  private fun parseScalarTypeExtension(): GQLScalarTypeExtension {
    val start = token
    expectKeyword("extend")
    expectKeyword("scalar")
    val name = parseName()
    val directives = parseDirectives(const = true)

    if (directives.isEmpty()) {
      unexpected()
    }

    return GQLScalarTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name,
        directives = directives
    )
  }


  private fun parseFieldDefinition(): GQLFieldDefinition {
    val start = token
    val description = parseDescription()
    val name = parseName()
    val argumentDefinitions = parseArgumentDefinitions()
    expectToken<Token.Colon>()
    val type = parseTypeInternal()
    val directives = parseDirectives(const = true)

    return GQLFieldDefinition(
        sourceLocation = sourceLocation(start),
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

  private fun parseObjectTypeDefinition(start: Token): GQLTypeDefinition {
    val description = parseDescription()
    this.expectKeyword("type")
    val name = parseName()
    val interfaces = parseImplementsInterfaces()
    val directives = parseDirectives(const = true)
    val fields = parseFieldDefinitions()

    return GQLObjectTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields,
    )
  }

  private fun parseObjectTypeExtension(): GQLObjectTypeExtension {
    val start = token
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
        sourceLocation = sourceLocation(start),
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields,
    )
  }

  private fun parseInterfaceTypeDefinition(start: Token): GQLInterfaceTypeDefinition {
    val description = parseDescription()
    expectKeyword("interface")
    val name = parseName()
    val interfaces = parseImplementsInterfaces()
    val directives = parseDirectives(const = true)
    val fields = parseFieldDefinitions()

    return GQLInterfaceTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields
    )
  }

  private fun parseInterfaceTypeExtension(): GQLInterfaceTypeExtension {
    val start = token
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
        sourceLocation = sourceLocation(start),
        name = name,
        implementsInterfaces = interfaces,
        directives = directives,
        fields = fields
    )
  }

  private fun parseUnionTypeDefinition(start: Token): GQLUnionTypeDefinition {
    val description = parseDescription()
    expectKeyword("union")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val memberTypes = parseUnionMemberTypes()

    return GQLUnionTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        directives = directives,
        memberTypes = memberTypes
    )
  }

  private fun parseUnionTypeExtension(): GQLUnionTypeExtension {
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
        sourceLocation = sourceLocation(start),
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

  private fun parseInputObjectTypeDefinition(start: Token): GQLInputObjectTypeDefinition {
    val description = parseDescription()
    expectKeyword("input")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val inputFields = parseInputFieldDefinitions()

    return GQLInputObjectTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        directives = directives,
        inputFields = inputFields,
    )
  }

  private fun parseInputObjectTypeExtension(): GQLInputObjectTypeExtension {
    val start = token
    expectKeyword("extend")
    expectKeyword("input")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val inputFields = parseInputFieldDefinitions()

    if (directives.isEmpty() && inputFields.isEmpty()) {
      unexpected()
    }

    return GQLInputObjectTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name,
        directives = directives,
        inputFields = inputFields,
    )
  }

  private fun parseInputFieldDefinitions(): List<GQLInputValueDefinition> {
    return parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLInputValueDefinition>(::parseInputValueDefinition).orEmpty()
  }

  private fun parseEnumTypeDefinition(start: Token): GQLEnumTypeDefinition {
    val description = parseDescription()
    expectKeyword("enum")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val values = parseEnumValueDefinitions()

    return GQLEnumTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        directives = directives,
        enumValues = values
    )
  }

  private fun parseEnumTypeExtension(): GQLEnumTypeExtension {
    val start = token
    expectKeyword("extend")
    expectKeyword("enum")
    val name = parseName()
    val directives = parseDirectives(const = true)
    val values = parseEnumValueDefinitions()

    if (directives.isEmpty() && values.isEmpty()) {
      unexpected()
    }

    return GQLEnumTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name,
        directives = directives,
        enumValues = values
    )
  }

  private fun parseEnumValueDefinitions(): List<GQLEnumValueDefinition> {
    return parseNonEmptyListOrNull<Token.LeftBrace, Token.RightBrace, GQLEnumValueDefinition>(::parseEnumValueDefinition).orEmpty()
  }

  private fun parseEnumValueDefinition(): GQLEnumValueDefinition {
    val start = token
    val description = parseDescription()
    val name = parseEnumValueName()
    val directives = parseDirectives(const = true)

    return GQLEnumValueDefinition(
        sourceLocation = sourceLocation(start),
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

  private fun parseDirectiveDefinition(start: Token): GQLDirectiveDefinition {
    val description = parseDescription()
    expectKeyword("directive")
    expectToken<Token.At>()
    val name = parseName()
    val arguments = parseArgumentDefinitions()
    val directives = if (!allowDirectivesOnDirectives) {
      if (token is Token.At) {
        throw ParserException("Experimental `allowDirectivesOnDirectives` must be set to true to allow directives on directives", token)
      } else {
        emptyList()
      }
    } else {
      parseDirectives(const = true)
    }
    val repeatable = expectOptionalKeyword("repeatable") != null
    expectKeyword("on")
    val locations = parseDirectiveLocations()

    return GQLDirectiveDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        arguments = arguments,
        repeatable = repeatable,
        locations = locations,
        directives = directives
    )
  }

  private fun parseDirectiveExtension(): GQLDirectiveExtension {
    val start = token
    expectKeyword("extend")
    expectKeyword("directive")
    expectToken<Token.At>()
    val name = parseName()
    val directives = parseDirectives(const = true)
    if (directives.isEmpty()) {
      unexpected()
    }
    return GQLDirectiveExtension(
        sourceLocation = sourceLocation(start),
        name = name,
        directives = directives
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
      "directive" -> if (!allowDirectivesOnDirectives) {
        throw ParserException("Experimental `allowDirectivesOnDirectives` must be set to true to allow directive extensions", t)
      } else {
        parseDirectiveExtension()
      }
      "service" -> {
        if (!allowServiceCapabilities) {
          unexpected(t)
        }
        parseServiceExtension()
      }
      else -> unexpected(t)
    }
  }

  private fun parseDefinition(): GQLDefinition {
    val start = token
    val hasDescription = peek<Token.String>()
    val t = if (hasDescription) lookaheadToken() else token

    return when (t) {
      is Token.LeftBrace -> parseOperationDefinition(start)
      is Token.Name -> {
        when (t.value) {
          "schema" -> parseSchemaDefinition(start)
          "scalar" -> parseScalarTypeDefinition(start)
          "type" -> parseObjectTypeDefinition(start)
          "interface" -> parseInterfaceTypeDefinition(start)
          "union" -> parseUnionTypeDefinition(start)
          "enum" -> parseEnumTypeDefinition(start)
          "input" -> parseInputObjectTypeDefinition(start)
          "directive" -> parseDirectiveDefinition(start)
          "query", "mutation", "subscription" -> parseOperationDefinition(start)
          "fragment" -> parseFragmentDefinition(start)
          "extend" -> {
            if (hasDescription) {
              throw ParserException("Type system extensions cannot have a description", t)
            }
            parseTypeSystemExtension()
          }
          "service" -> {
            if (!allowServiceCapabilities) {
              unexpected(t)
            }
            parseServiceDefinition(start)
          }
          else -> unexpected(t)
        }
      }

      else -> unexpected()
    }
  }

  private fun parseFragmentDefinition(start: Token): GQLFragmentDefinition {
    val description = parseDescription()
    expectKeyword("fragment")
    val name = parseFragmentName()
    val variableDefinitions = if (allowFragmentArguments) {
      parseVariableDefinitions()
    } else {
      emptyList()
    }
    expectKeyword("on")
    val typeCondition = parseNamedType()
    val directives = parseDirectives(const = false)
    val selections = parseSelectionSet()
    return GQLFragmentDefinition(
        sourceLocation = sourceLocation(start),
        description = description,
        name = name,
        typeCondition = typeCondition,
        directives = directives,
        selections = selections,
        variableDefinitions = variableDefinitions
    )
  }

  private fun parseArgumentDefinitions(): List<GQLInputValueDefinition> {
    return parseNonEmptyListOrNull<Token.LeftParenthesis, Token.RightParenthesis, GQLInputValueDefinition>(::parseInputValueDefinition).orEmpty()
  }

  private fun parseInputValueDefinition(): GQLInputValueDefinition {
    val start = token
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
        sourceLocation = sourceLocation(start),
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
    }.also {
      if (!allowDirectivesOnDirectives && it == GQLDirectiveLocation.DIRECTIVE_DEFINITION) {
        throw ParserException("Experimental `allowDirectivesOnDirectives` must be set to true to allow directives on directives", start)
      }
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
    val start = token

    expectToken<Token.At>()
    val name = parseName()
    val args = parseArguments(const)
    return GQLDirective(sourceLocation(start), name, args)
  }

  private fun parseArguments(const: Boolean): List<GQLArgument> {
    return parseNonEmptyListOrNull<Token.LeftParenthesis, Token.RightParenthesis, GQLArgument> { parseArgument(const) }.orEmpty()
  }

  private fun parseArgument(const: Boolean): GQLArgument {
    val start = token
    val name = parseName()

    expectToken<Token.Colon>()
    val value = parseValueInternal(const)
    return GQLArgument(
        sourceLocation = sourceLocation(start),
        name = name,
        value = value
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

  private fun sourceLocation(): SourceLocation? {
    if (!withSourceLocation) return null

    return SourceLocation(
        token.start,
        token.end,
        token.line,
        token.column,
        filePath
    )
  }

  private fun parseValueInternal(const: Boolean): GQLValue {
    return when (val t = token) {
      is Token.LeftBracket -> parseList(const)
      is Token.LeftBrace -> parseObject(const)

      is Token.Int -> GQLIntValue(sourceLocation(), t.value).also { advance() }
      is Token.Float -> GQLFloatValue(sourceLocation(), t.value).also { advance() }
      is Token.String -> GQLStringValue(sourceLocation(), t.value).also { advance() }
      is Token.Name -> when (t.value) {
        "true" -> GQLBooleanValue(sourceLocation(), true).also { advance() }
        "false" -> GQLBooleanValue(sourceLocation(), false).also { advance() }
        "null" -> GQLNullValue(sourceLocation()).also { advance() }
        else -> GQLEnumValue(sourceLocation(), t.value).also { advance() }
      }

      is Token.Dollar -> {
        val start = token
        advance()
        if (const) {
          val n = expectOptionalToken<Token.Name>()
          if (n != null) {
            throw ParserException("Unexpected variable '${n.value}' in constant value.", n)
          } else {
            unexpected(n)
          }
        }
        val name = parseName()
        GQLVariableValue(
            sourceLocation = sourceLocation(start),
            name = name
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
    val start = token
    val name = parseName()
    expectToken<Token.Colon>()
    val value = parseValueInternal(const)
    return GQLObjectField(
        sourceLocation = sourceLocation(start),
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

  private fun parseSchemaCoordinateInternal(): GQLSchemaCoordinate {
    val sourceLocation = sourceLocation()
    return if (token is Token.At) {
      advance()
      val name = expectToken<Token.Name>().value
      if (token is Token.LeftParenthesis) {
        advance()
        val argument = expectToken<Token.Name>().value
        expectToken<Token.Colon>()
        expectToken<Token.RightParenthesis>()
        GQLDirectiveArgumentCoordinate(sourceLocation, name, argument)
      } else {
        GQLDirectiveCoordinate(sourceLocation, name)
      }
    } else {
      val name = expectToken<Token.Name>().value
      if (token is Token.Dot) {
        advance()
        val member = expectToken<Token.Name>().value
        if (token is Token.LeftParenthesis) {
          advance()
          val argument = expectToken<Token.Name>().value
          expectToken<Token.Colon>()
          expectToken<Token.RightParenthesis>()
          GQLArgumentCoordinate(sourceLocation, name, member, argument)
        } else {
          GQLMemberCoordinate(sourceLocation, name, member)
        }
      } else {
        GQLTypeCoordinate(sourceLocation, name)
      }
    }
  }

  private fun parseTypeInternal(): GQLType {
    val start = token

    val type = if (expectOptionalToken<Token.LeftBracket>() != null) {
      val ofType = parseTypeInternal()
      expectToken<Token.RightBracket>()

      GQLListType(
          sourceLocation = sourceLocation(start),
          type = ofType
      )
    } else {
      parseNamedType()
    }

    return if (token is Token.ExclamationPoint) {
      advance()
      GQLNonNullType(
          sourceLocation = sourceLocation(start),
          type
      )
    } else {
      type
    }
  }
}
