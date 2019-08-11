package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.ir.*
import com.apollographql.apollo.compiler.parser.GraphQLDocumentSourceBuilder.graphQLDocumentSource
import com.apollographql.apollo.compiler.parser.antlr.GraphQLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.File
import java.io.IOException

class GraphQLDocumentParser(val schema: Schema) {
  private val typenameField = Field(
      responseName = "__typename",
      fieldName = "__typename",
      type = "String!"
  )

  fun parse(graphQLFiles: List<File>): CodeGenerationIR {
    val (operations, fragments, usedTypes) = graphQLFiles.fold(DocumentParseResult()) { acc, graphQLFile ->
      val result = graphQLFile.parse()
      DocumentParseResult(
          operations = acc.operations + result.operations,
          fragments = acc.fragments + result.fragments,
          usedTypes = acc.usedTypes.union(result.usedTypes)
      )
    }

    val typeDeclarations = usedTypes.usedTypeDeclarations()

    return CodeGenerationIR(
        operations = operations.map { operation ->
          val operationReferencedFragments = fragments.filter { it.fragmentName in operation.fragmentsReferenced }
          val transientReferencedFragmentNames = operationReferencedFragments.flatMap { it.fragmentsReferenced }
          val transientReferencedFragments = fragments.filter { it.fragmentName in transientReferencedFragmentNames }
          val fragmentSource = (operationReferencedFragments + transientReferencedFragments).joinToString(separator = "\n") {
            it.source
          }
          operation.copy(sourceWithFragments = operation.source + if (fragmentSource.isNotBlank()) "\n$fragmentSource" else "")
        },
        fragments = fragments,
        typesUsed = typeDeclarations
    )
  }

  private fun File.parse(): DocumentParseResult {
    val document = try {
      readText()
    } catch (e: IOException) {
      throw RuntimeException("Failed to read GraphQL file `$this`", e)
    }

    val tokenStream = GraphQLLexer(ANTLRInputStream(document))
        .apply { removeErrorListeners() }
        .let { CommonTokenStream(it) }

    val parser = GraphQLParser(tokenStream).apply {
      interpreter.predictionMode = PredictionMode.LL
      removeErrorListeners()
      addErrorListener(object : BaseErrorListener() {
        override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, position: Int, msg: String?,
                                 e: RecognitionException?) {
          throw GraphQLDocumentParseException(
              graphQLFilePath = absolutePath,
              document = document,
              parseException = ParseException(
                  message = "Unsupported token `${(offendingSymbol as? Token)?.text ?: offendingSymbol.toString()}`",
                  line = line,
                  position = position
              )
          )
        }
      })
    }

    try {
      return parser.document()
          .also { ctx -> parser.checkEOF(ctx) }
          .let { ctx -> ctx.parse(path) }
    } catch (e: ParseException) {
      throw GraphQLDocumentParseException(
          graphQLFilePath = absolutePath,
          document = document,
          parseException = e
      )
    }
  }

  private fun GraphQLParser.checkEOF(documentContext: GraphQLParser.DocumentContext) {
    val documentStopToken = documentContext.getStop()
    val allTokens = (tokenStream as CommonTokenStream).tokens
    if (documentStopToken != null && !allTokens.isNullOrEmpty()) {
      val lastToken = allTokens[allTokens.size - 1]
      val eof = lastToken.type == Token.EOF
      val sameChannel = lastToken.channel == documentStopToken.channel
      if (!eof && lastToken.tokenIndex > documentStopToken.tokenIndex && sameChannel) {
        throw ParseException(
            message = "Unsupported token `${lastToken.text}`",
            line = lastToken.line,
            position = lastToken.charPositionInLine
        )
      }
    }
  }

  private fun GraphQLParser.DocumentContext.parse(graphQLFilePath: String): DocumentParseResult {
    val fragments = definition().mapNotNull { ctx ->
      ctx.fragmentDefinition()?.parse(schema)
    }
    val operations = definition().mapNotNull { ctx ->
      ctx.operationDefinition()?.parse(graphQLFilePath)
    }
    return DocumentParseResult(
        operations = operations.map { it.result },
        fragments = fragments.map { it.result },
        usedTypes = fragments.flatMap { it.usedTypes }.union(operations.flatMap { it.usedTypes })
    )
  }

  private fun GraphQLParser.OperationDefinitionContext.parse(graphQLFilePath: String): ParseResult<Operation> {
    val operationType = operationType().text
    val operationName = NAME().text
    val variables = variableDefinitions().parse()
    val schemaType = operationType().schemaType()
    val fields = selectionSet().parse(schemaType).also { fields ->
      if (fields.result.isEmpty()) {
        throw ParseException(
            message = "GraphQL operation `$operationName` of type `$operationType` must have a selection of subfields",
            token = operationType().start
        )
      }
    }
    return ParseResult(
        result = Operation(
            operationName = operationName,
            operationType = operationType,
            variables = variables.result,
            source = graphQLDocumentSource,
            sourceWithFragments = graphQLDocumentSource,
            fields = fields.result.minus(typenameField),
            fragmentsReferenced = fields.result.referencedFragments().distinct(),
            filePath = graphQLFilePath,
            operationId = ""
        ),
        usedTypes = variables.usedTypes + fields.usedTypes
    )
  }

  private fun GraphQLParser.OperationTypeContext.schemaType(): Schema.Type {
    val operationRoot = when (text.toLowerCase()) {
      "query" -> schema.queryType
      "mutation" -> schema.mutationType
      "subscription" -> schema.subscriptionType
      else -> throw ParseException(
          message = "Unknown GraphQL operation type `$text`",
          token = start
      )
    }
    return schema[operationRoot] ?: throw ParseException(
        message = "Can't resolve root for `$text` operation type",
        token = start
    )
  }

  private fun GraphQLParser.VariableDefinitionsContext?.parse(): ParseResult<List<Variable>> {
    return this
        ?.variableDefinition()
        ?.map { ctx -> ctx.parse() }
        ?.flatten()
        ?: ParseResult(result = emptyList(), usedTypes = emptySet())
  }

  // TODO validate variable type
  private fun GraphQLParser.VariableDefinitionContext.parse(): ParseResult<Variable> {
    val name = variable().NAME().text
    val type = type().text
    return ParseResult(
        result = Variable(name = name, type = type),
        usedTypes = setOf(type.replace("[", "").replace("]", "").replace("!", ""))
    )
  }

  private fun GraphQLParser.SelectionSetContext?.parse(schemaType: Schema.Type): ParseResult<List<Field>> {
    val hasInlineFragments = this?.selection()?.find { it.inlineFragment() != null } != null
    val hasFragments = this?.selection()?.find { it.fragmentSpread() != null } != null
    val hasFields = this?.selection()?.find { it.field() != null } != null
    return this
        ?.selection()
        ?.mapNotNull { ctx -> ctx.field()?.parse(schemaType) }
        ?.flatten()
        ?.let { (fields, usedTypes) ->
          val withTypenameField = (hasFields || hasInlineFragments || hasFragments) && !fields.contains(typenameField)
          ParseResult(
              result = (if (withTypenameField) listOf(typenameField) else emptyList()) + fields,
              usedTypes = usedTypes
          )
        }
        ?: ParseResult(result = emptyList(), usedTypes = emptySet())
  }

  private fun GraphQLParser.FieldContext.parse(schemaType: Schema.Type): ParseResult<Field> {
    val responseName = fieldName().alias()?.NAME(0)?.text ?: fieldName().NAME().text
    val fieldName = fieldName().alias()?.NAME(1)?.text ?: fieldName().NAME().text
    if (fieldName == typenameField.fieldName) {
      return ParseResult(result = typenameField, usedTypes = emptySet())
    }
    val schemaField = schemaType.lookupField(
        fieldName = fieldName,
        token = fieldName().alias()?.NAME(1)?.symbol ?: fieldName().NAME().symbol
    )
    val schemaFieldType = schema[schemaField.type.rawType.name] ?: throw ParseException(
        message = "Unknown GraphQL type `${schemaField.type.rawType.name}`",
        token = fieldName().alias()?.NAME(1)?.symbol ?: fieldName().NAME().symbol
    )
    val arguments = arguments().parse(schemaField)
    val fields = selectionSet().parse(schemaFieldType)
    val fragmentSpreads = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.fragmentSpread()?.fragmentName()?.NAME()?.text
    } ?: emptyList()
    val inlineFragments = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.inlineFragment()?.parse(selectionSet())
    } ?: emptyList()
    val conditions = directives().parse()
    return ParseResult(
        result = Field(
            responseName = responseName,
            fieldName = fieldName,
            type = schemaField.type.asIrType(),
            args = arguments.result,
            isConditional = conditions.isNotEmpty(),
            fields = fields.result,
            fragmentSpreads = fragmentSpreads,
            inlineFragments = inlineFragments.map { it.result },
            description = schemaField.description?.trim(),
            isDeprecated = schemaField.isDeprecated,
            deprecationReason = schemaField.deprecationReason,
            conditions = conditions
        ),
        usedTypes = setOf(schemaField.type.rawType.name!!)
            .union(arguments.usedTypes)
            .union(fields.usedTypes)
            .union(inlineFragments.flatMap { it.usedTypes })
    )
  }

  private fun GraphQLParser.DirectivesContext?.parse(): List<Condition> {
    return this?.directive()?.mapNotNull { ctx ->
      val name = ctx.NAME().text
      val argument = ctx.argument()
      when {
        argument == null -> null
        name != "skip" && name != "include" -> null
        argument.NAME()?.text != "if" || argument.valueOrVariable()?.variable() == null -> null
        else -> Condition(
            kind = "BooleanCondition",
            variableName = argument.valueOrVariable().variable().NAME().text,
            inverted = ctx.NAME().text == "skip"
        )
      }
    } ?: emptyList()
  }

  private fun GraphQLParser.ArgumentsContext?.parse(schemaField: Schema.Field): ParseResult<List<Argument>> {
    return this
        ?.argument()
        ?.map { ctx -> ctx.parse(schemaField) }
        ?.flatten()
        ?: ParseResult(result = emptyList(), usedTypes = emptySet())
  }

  private fun GraphQLParser.ArgumentContext.parse(schemaField: Schema.Field): ParseResult<Argument> {
    val name = NAME().text
    val schemaArgument = schemaField.args.find { it.name == name } ?: throw ParseException(
        message = "Unknown argument `$name` on field `${schemaField.name}`",
        token = NAME().symbol
    )
    val type = schemaArgument.type.asIrType()
    val value = valueOrVariable().variable()?.let {
      mapOf(
          "kind" to "Variable",
          "variableName" to it.NAME().text
      )
    } ?: valueOrVariable().value().parse()
    return ParseResult(
        result = Argument(
            name = name,
            type = type,
            value = value
        ),
        usedTypes = emptySet()
    )
  }

  private fun GraphQLParser.InlineFragmentContext.parse(parentSelectionSet: GraphQLParser.SelectionSetContext): ParseResult<InlineFragment> {
    val typeCondition = typeCondition().typeName().NAME().text
    val schemaType = schema[typeCondition] ?: throw ParseException(
        message = "Can't resolve inline fragment on `$typeCondition}`",
        token = typeCondition().typeName().NAME().symbol
    )

    val possibleTypes = when (schemaType) {
      is Schema.Type.Interface -> schemaType.possibleTypes?.map { it.rawType.name!! } ?: emptyList()
      is Schema.Type.Union -> schemaType.possibleTypes?.map { it.rawType.name!! } ?: emptyList()
      else -> listOf(typeCondition)
    }.distinct()

    val fields = parentSelectionSet.parse(schemaType).plus(
        selectionSet().parse(schemaType)
    ) { left, right -> left.union(right) }
    if (fields.result.isEmpty()) {
      throw ParseException(
          message = "Inline fragment `$typeCondition` must have a selection of subfields",
          token = typeCondition().typeName().NAME().symbol
      )
    }

    val fragmentSpreads = selectionSet()?.selection()?.mapNotNull { selection ->
      selection.fragmentSpread()?.fragmentName()?.NAME()?.text
    } ?: emptyList()

    return ParseResult(
        result = InlineFragment(
            typeCondition = typeCondition,
            possibleTypes = possibleTypes,
            fields = fields.result,
            fragmentSpreads = fragmentSpreads
        ),
        usedTypes = setOf(typeCondition).union(fields.usedTypes)
    )
  }

  private fun GraphQLParser.FragmentDefinitionContext.parse(schema: Schema): ParseResult<Fragment> {
    val fragmentName = fragmentName().text
    val typeCondition = typeCondition().typeName().NAME().text
    val schemaType = schema[typeCondition] ?: throw ParseException(
        message = "Unknown GraphQL type `$typeCondition`",
        token = typeCondition().typeName().NAME().symbol
    )

    val possibleTypes = when (schemaType) {
      is Schema.Type.Interface -> schemaType.possibleTypes?.map { it.rawType.name!! } ?: emptyList()
      is Schema.Type.Union -> schemaType.possibleTypes?.map { it.rawType.name!! } ?: emptyList()
      else -> listOf(typeCondition)
    }.distinct()

    val fields = selectionSet().parse(schemaType)
    if (fields.result.isEmpty()) {
      throw ParseException(
          message = "Fragment `$fragmentName` must have a selection of subfields",
          token = fragmentName().start
      )
    }
    val fragmentSpreads = selectionSet()?.selection()?.mapNotNull { selection ->
      selection.fragmentSpread()?.fragmentName()?.NAME()?.text
    } ?: emptyList()

    val inlineFragments = selectionSet()?.selection()?.mapNotNull { selection ->
      selection.inlineFragment()?.parse(selectionSet())
    } ?: emptyList()

    return ParseResult(
        result = Fragment(
            fragmentName = fragmentName,
            typeCondition = typeCondition,
            source = graphQLDocumentSource,
            possibleTypes = possibleTypes,
            fields = fields.result,
            fragmentSpreads = fragmentSpreads,
            fragmentsReferenced = fields.result.referencedFragments(),
            inlineFragments = inlineFragments.map { it.result }
        ),
        usedTypes = setOf(typeCondition)
            .union(fields.usedTypes)
            .union(inlineFragments.flatMap { it.usedTypes })
    )
  }

  private fun GraphQLParser.ValueContext.parse(): Any = when (this) {
    is GraphQLParser.NumberValueContext -> NUMBER().text.trim().toDouble()
    is GraphQLParser.BooleanValueContext -> BOOLEAN().text.trim().toBoolean()
    is GraphQLParser.StringValueContext -> STRING().text.trim().replace("\"", "")
    is GraphQLParser.LiteralValueContext -> NAME().text
    is GraphQLParser.ArrayValueContext -> array().value().map { value -> value.parse() }
    is GraphQLParser.InlineInputTypeValueContext -> {
      inlineInputType().inlineInputTypeField().map { field ->
        val name = field.NAME().text
        val variableValue = field.valueOrVariable().variable()?.NAME()?.text
        val value = field.valueOrVariable().value()?.parse()
        name to when {
          variableValue != null -> mapOf(
              "kind" to "Variable",
              "variableName" to variableValue
          )
          else -> value
        }
      }.toMap()
    }
    else -> throw ParseException(
        message = "Unsupported argument value `$text`",
        token = start
    )
  }

  private fun Schema.Type.lookupField(fieldName: String, token: Token): Schema.Field = when (this) {
    is Schema.Type.Interface -> fields?.find { it.name == fieldName }
    is Schema.Type.Object -> fields?.find { it.name == fieldName }
    is Schema.Type.Union -> fields?.find { it.name == fieldName }
    else -> throw ParseException(
        message = "Can't query `$fieldName` on type `$name`. `$name` is not one of the expected types: `INTERFACE`, `OBJECT`, `UNION`.",
        token = token
    )
  } ?: throw ParseException(
      message = "Can't query `$fieldName` on type `$name`",
      token = token
  )

  private fun Schema.TypeRef.asIrType(): String = when (kind) {
    Schema.Kind.LIST -> "[${ofType!!.asIrType()}]"
    Schema.Kind.NON_NULL -> "${ofType!!.asIrType()}!"
    else -> name!!
  }

  private fun Set<String>.usedTypeDeclarations(): List<TypeDeclaration> {
    return usedSchemaTypes().map { type ->
      TypeDeclaration(
          kind = when (type.kind) {
            Schema.Kind.SCALAR -> "ScalarType"
            Schema.Kind.ENUM -> "EnumType"
            Schema.Kind.INPUT_OBJECT -> "InputObjectType"
            else -> null
          }!!,
          name = type.name,
          description = type.description?.trim(),
          values = (type as? Schema.Type.Enum)?.enumValues?.map { value ->
            TypeDeclarationValue(
                name = value.name,
                description = value.description?.trim(),
                isDeprecated = value.isDeprecated || !value.deprecationReason.isNullOrBlank(),
                deprecationReason = value.deprecationReason
            )
          },
          fields = (type as? Schema.Type.InputObject)?.inputFields?.map { field ->
            TypeDeclarationField(
                name = field.name,
                description = field.description?.trim(),
                type = field.type.asIrType(),
                defaultValue = field.defaultValue.normalizeValue(field.type)
            )
          }
      )
    }
  }

  private fun Set<String>.usedSchemaTypes(): List<Schema.Type> {
    val types = filter { ScalarType.forName(it) == null }
        .map {
          schema[it] ?: throw ParseException(
              message = "Unknown type `$it`",
              position = -1,
              line = -1
          )
        }
        .filter { type -> type.kind == Schema.Kind.SCALAR || type.kind == Schema.Kind.ENUM || type.kind == Schema.Kind.INPUT_OBJECT }
    val inputObjectTransientTypes = types
        .mapNotNull { type -> type as? Schema.Type.InputObject }
        .flatMap { inputObject ->
          inputObject
              .inputFields
              .map { field -> field.type.rawType.name!! }
              .filter { ScalarType.forName(it) == null }
              .map { type ->
                schema[type] ?: throw ParseException(
                    message = "Unknown type `$type`",
                    position = -1,
                    line = -1
                )
              }
              .filter { type -> type.kind == Schema.Kind.SCALAR || type.kind == Schema.Kind.ENUM || type.kind == Schema.Kind.INPUT_OBJECT }
        }
    return types + inputObjectTransientTypes
  }

  private fun List<Field>.union(other: List<Field>): List<Field> {
    val fieldNames = map { it.responseName + ":" + it.fieldName }
    return map { targetField ->
      val targetFieldName = targetField.responseName + ":" + targetField.fieldName
      targetField.copy(
          fields = targetField.fields?.union(
              other.find { otherField ->
                otherField.responseName + ":" + otherField.fieldName == targetFieldName
              }?.fields ?: emptyList()
          )
      )
    } + other.filter { (it.responseName + ":" + it.fieldName) !in fieldNames }
  }

  private fun List<Field>.referencedFragments(): List<String> {
    return mapNotNull { it.fragmentSpreads }.flatten() +
        mapNotNull { it.fields?.referencedFragments() }.flatten() +
        mapNotNull { field -> field.inlineFragments?.flatMap { it.fragmentSpreads ?: emptyList() } }.flatten() +
        mapNotNull { field -> field.inlineFragments?.flatMap { it.fields.referencedFragments() } }.flatten()
  }

  private fun Any?.normalizeValue(type: Schema.TypeRef): Any? {
    if (this == null) {
      return null
    }
    return when (type.kind) {
      Schema.Kind.SCALAR -> {
        when (ScalarType.forName(type.name ?: "")) {
          ScalarType.INT -> toString().trim().toInt()
          ScalarType.BOOLEAN -> toString().trim().toBoolean()
          ScalarType.FLOAT -> toString().trim().toDouble()
          else -> toString()
        }
      }
      Schema.Kind.NON_NULL -> normalizeValue(type.ofType!!)
      Schema.Kind.LIST -> {
        //TODO: remove this restriction required for backward compatibility
        if (type.rawType.kind == Schema.Kind.ENUM) {
          null
        } else {
          toString().removePrefix("[").removeSuffix("]").split(',').map { value ->
            value.trim().replace("\"", "").normalizeValue(type.ofType!!)
          }
        }
      }
      else -> toString()
    }
  }

  private fun <T> List<ParseResult<T>>.flatten() = ParseResult(
      result = map { (result, _) -> result },
      usedTypes = flatMap { (_, usedTypes) -> usedTypes }.toSet()
  )
}

private data class DocumentParseResult(
    val operations: List<Operation> = emptyList(),
    val fragments: List<Fragment> = emptyList(),
    val usedTypes: Set<String> = emptySet()
)

private data class ParseResult<T>(
    val result: T,
    val usedTypes: Set<String> = emptySet()
) {
  fun <R> plus(other: ParseResult<T>, combine: (T, T) -> R) = ParseResult(
      result = combine(result, other.result),
      usedTypes = usedTypes + other.usedTypes
  )
}


private class ParseException(message: String, val line: Int, val position: Int) : RuntimeException(message) {
  companion object {
    operator fun invoke(message: String, token: Token) = ParseException(
        message = message,
        line = token.line,
        position = token.charPositionInLine
    )
  }
}

private class GraphQLDocumentParseException(
    graphQLFilePath: String,
    document: String,
    parseException: ParseException
) : RuntimeException(preview(
    graphQLFilePath = graphQLFilePath,
    document = document,
    parseException = parseException
), parseException) {

  override fun fillInStackTrace(): Throwable = this

  companion object {
    private fun preview(graphQLFilePath: String, document: String, parseException: ParseException): String {
      val documentLines = document.lines()
      return "\nFailed to parse GraphQL file $graphQLFilePath (${parseException.line}:${parseException.position})\n${parseException.message}" +
          "\n----------------------------------------------------\n" +
          parseException.let { error ->
            val prefix = if (error.line - 2 >= 0) {
              "[${error.line - 1}]:" + documentLines[error.line - 2]
            } else ""
            val body = if (error.line - 1 >= 0) {
              "\n[${error.line}]:${documentLines[error.line - 1]}\n"
            } else ""
            val postfix = if (error.line < documentLines.size) {
              "[${error.line + 1}]:" + documentLines[error.line]
            } else ""
            "$prefix$body$postfix"
          } +
          "\n----------------------------------------------------"
    }
  }
}
