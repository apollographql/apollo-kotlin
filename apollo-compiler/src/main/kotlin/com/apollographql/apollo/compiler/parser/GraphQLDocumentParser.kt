package com.apollographql.apollo.compiler.parser

import com.apollographql.apollo.compiler.formatPackageName
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

    operations.checkMultipleOperationDefinitions()
    fragments.checkMultipleFragmentDefinitions()

    val typeDeclarations = usedTypes.usedTypeDeclarations()

    return CodeGenerationIR(
        operations = operations.map { operation ->
          val referencedFragmentNames = operation.fields.referencedFragmentNames(fragments = fragments, filePath = operation.filePath)
          val referencedFragments = referencedFragmentNames.mapNotNull { fragmentName -> fragments.find { it.fragmentName == fragmentName } }
          referencedFragments.forEach { it.checkVariableDefinitions(operation) }

          val fragmentSource = referencedFragments.joinToString(separator = "\n") { it.source }
          operation.copy(
              sourceWithFragments = operation.source + if (fragmentSource.isNotBlank()) "\n$fragmentSource" else "",
              fragmentsReferenced = referencedFragmentNames.toList()
          )
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
                  sourceLocation = SourceLocation(
                      line = line,
                      position = position
                  )
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
            token = lastToken
        )
      }
    }
  }

  private fun GraphQLParser.DocumentContext.parse(graphQLFilePath: String): DocumentParseResult {
    val fragments = definition().mapNotNull { it.fragmentDefinition()?.parse(graphQLFilePath) }
    val operations = definition().mapNotNull { ctx ->
      ctx.operationDefinition()?.parse(graphQLFilePath)?.also {
        it.result.checkVariableDefinitions()
      }
    }
    return DocumentParseResult(
        operations = operations.map { it.result },
        fragments = fragments.map { it.result },
        usedTypes = fragments.flatMap { it.usedTypes }.union(operations.flatMap { it.usedTypes })
    )
  }

  private fun GraphQLParser.OperationDefinitionContext.parse(graphQLFilePath: String): ParseResult<Operation> {
    val operationType = operationType().text
    val operationName = NAME()?.text ?: throw ParseException(
        message = "Apollo does not support anonymous operations",
        token = operationType().start
    )
    val variables = variableDefinitions().parse()
    val schemaType = operationType().schemaType()
    val fields = selectionSet().parse(schemaType).also { fields ->
      if (fields.result.isEmpty()) {
        throw ParseException(
            message = "Operation `$operationName` of type `$operationType` must have a selection of sub-fields",
            token = operationType().start
        )
      }
    }
    val operation = Operation(
        operationName = operationName,
        operationType = operationType,
        variables = variables.result,
        source = graphQLDocumentSource,
        sourceWithFragments = graphQLDocumentSource,
        fields = fields.result.minus(typenameField),
        fragmentsReferenced = emptyList(),
        filePath = graphQLFilePath,
        operationId = ""
    ).also { it.checkVariableDefinitions() }

    return ParseResult(
        result = operation,
        usedTypes = variables.usedTypes + fields.usedTypes
    )
  }

  private fun GraphQLParser.OperationTypeContext.schemaType(): Schema.Type {
    val operationRoot = when (text.toLowerCase()) {
      "query" -> schema.queryType
      "mutation" -> schema.mutationType
      "subscription" -> schema.subscriptionType
      else -> throw ParseException(
          message = "Unknown operation type `$text`",
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

  private fun GraphQLParser.VariableDefinitionContext.parse(): ParseResult<Variable> {
    val name = variable().NAME().text
    val type = type().text
    val schemaType = schema[type.replace("!", "").replace("[", "").replace("]", "")] ?: throw ParseException(
        message = "Unknown variable type `$type`",
        token = type().start
    )
    return ParseResult(
        result = Variable(
            name = name,
            type = type,
            sourceLocation = SourceLocation(variable().NAME().symbol)
        ),
        usedTypes = setOf(schemaType.name)
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
        message = "Unknown type `${schemaField.type.rawType.name}`",
        token = fieldName().alias()?.NAME(1)?.symbol ?: fieldName().NAME().symbol
    )
    val arguments = arguments().parse(schemaField)
    val fields = selectionSet().parse(schemaFieldType)
    val fragmentSpreads = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.fragmentSpread()?.fragmentName()?.NAME()?.text
    } ?: emptyList()
    val inlineFragments = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.inlineFragment()?.parse(parentSelectionSet = selectionSet(), parentSchemaType = schemaFieldType)
    }?.flatten() ?: ParseResult(result = emptyList())

    val mergeInlineFragmentFields = inlineFragments.result
        .filter { it.typeCondition == schemaFieldType.name }
        .flatMap { it.fields }
        .filter { it != typenameField }
    val mergeInlineFragmentSpreadFragments = inlineFragments.result
        .filter { it.typeCondition == schemaFieldType.name }
        .flatMap { it.fragmentSpreads ?: emptyList() }

    val conditions = directives().parse()
    return ParseResult(
        result = Field(
            responseName = responseName,
            fieldName = fieldName,
            type = schemaField.type.asIrType(),
            args = arguments.result,
            isConditional = conditions.isNotEmpty(),
            fields = fields.result.mergeFields(other = mergeInlineFragmentFields, parseContext = this),
            fragmentSpreads = fragmentSpreads.union(mergeInlineFragmentSpreadFragments).toList(),
            inlineFragments = inlineFragments.result.filter { it.typeCondition != schemaFieldType.name },
            description = schemaField.description?.trim(),
            isDeprecated = schemaField.isDeprecated,
            deprecationReason = schemaField.deprecationReason,
            conditions = conditions
        ),
        usedTypes = setOf(schemaField.type.rawType.name!!)
            .union(arguments.usedTypes)
            .union(fields.usedTypes)
            .union(inlineFragments.usedTypes)
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
            inverted = ctx.NAME().text == "skip",
            sourceLocation = SourceLocation(argument.valueOrVariable()?.start ?: argument.NAME().symbol)
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
    val value = valueOrVariable().variable()?.let { ctx ->
      mapOf(
          "kind" to "Variable",
          "variableName" to ctx.NAME().text
      )
    } ?: valueOrVariable().value().parse()
    return ParseResult(
        result = Argument(
            name = name,
            type = type,
            value = value,
            sourceLocation = SourceLocation(valueOrVariable().start)
        ),
        usedTypes = emptySet()
    )
  }

  private fun GraphQLParser.InlineFragmentContext.parse(
      parentSelectionSet: GraphQLParser.SelectionSetContext,
      parentSchemaType: Schema.Type
  ): ParseResult<InlineFragment> {
    val typeCondition = typeCondition().typeName().NAME().text
    val schemaType = schema[typeCondition] ?: throw ParseException(
        message = "Unknown type`$typeCondition}`",
        token = typeCondition().typeName().start
    )

    if (!parentSchemaType.isAssignableFrom(schemaType)) {
      throw ParseException(
          message = "Fragment cannot be spread here as objects of type `${parentSchemaType.name}` can never be of type `$typeCondition`",
          token = typeCondition().typeName().start
      )
    }

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
          message = "Inline fragment `$typeCondition` must have a selection of sub-fields",
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

  private fun GraphQLParser.FragmentDefinitionContext.parse(graphQLFilePath: String): ParseResult<Fragment> {
    val fragmentKeyword = fragmentKeyword().text
    if (fragmentKeyword != "fragment") {
      throw ParseException(
          message = "Unsupported token `$fragmentKeyword`",
          token = fragmentKeyword().start
      )
    }

    val fragmentName = fragmentName().NAME().text

    val typeCondition = typeCondition().typeName().NAME().text
    val schemaType = schema[typeCondition] ?: throw ParseException(
        message = "Unknown type `$typeCondition`",
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
          message = "Fragment `$fragmentName` must have a selection of sub-fields",
          token = fragmentName().NAME().symbol
      )
    }
    val fragmentSpreads = selectionSet()?.selection()?.mapNotNull { selection ->
      selection.fragmentSpread()?.fragmentName()?.NAME()?.text
    } ?: emptyList()

    val inlineFragments = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.inlineFragment()?.parse(parentSelectionSet = selectionSet(), parentSchemaType = schemaType)
    }?.flatten() ?: ParseResult(result = emptyList())

    val mergeInlineFragmentFields = inlineFragments.result
        .filter { it.typeCondition == typeCondition }
        .flatMap { it.fields }
        .filter { it != typenameField }
    val mergeInlineFragmentSpreadFragments = inlineFragments.result
        .filter { it.typeCondition == typeCondition }
        .flatMap { it.fragmentSpreads ?: emptyList() }

    return ParseResult(
        result = Fragment(
            fragmentName = fragmentName,
            typeCondition = typeCondition,
            source = graphQLDocumentSource,
            possibleTypes = possibleTypes,
            fields = fields.result.mergeFields(other = mergeInlineFragmentFields, parseContext = this),
            fragmentSpreads = fragmentSpreads.union(mergeInlineFragmentSpreadFragments).toList(),
            inlineFragments = inlineFragments.result.filter { it.typeCondition != typeCondition },
            filePath = graphQLFilePath
        ),
        usedTypes = setOf(typeCondition)
            .union(fields.usedTypes)
            .union(inlineFragments.usedTypes)
    )
  }

  private fun GraphQLParser.ValueContext.parse(): Any = when (this) {
    is GraphQLParser.NumberValueContext -> NUMBER().text.trim().toDouble()
    is GraphQLParser.BooleanValueContext -> BOOLEAN().text.trim().toBoolean()
    is GraphQLParser.StringValueContext -> STRING().text.trim().replace("\"", "")
    is GraphQLParser.LiteralValueContext -> NAME().text
    is GraphQLParser.ArrayValueContext -> {
      arrayValueType().valueOrVariable().map { valueOrVariable ->
        valueOrVariable.variable()?.let { variable ->
          mapOf(
              "kind" to "Variable",
              "variableName" to variable.NAME().text
          )
        } ?: valueOrVariable.value().parse()
      }
    }
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

  private fun Set<String>.usedSchemaTypes(): Set<Schema.Type> {
    if (isEmpty()) {
      return emptySet()
    }

    val usedSchemaTypes = filter { ScalarType.forName(it) == null }
        .map { schema[it] ?: throw GraphQLParseException(message = "Undefined schema type `$it`") }
        .filter { type -> type.kind == Schema.Kind.SCALAR || type.kind == Schema.Kind.ENUM || type.kind == Schema.Kind.INPUT_OBJECT }
        .toSet()

    val inputObjectUsedTypes = usedSchemaTypes
        .mapNotNull { type -> type as? Schema.Type.InputObject }
        .flatMap { inputObject -> inputObject.usedTypes(exclude = this) }
        .toSet()
        .usedSchemaTypes()

    return usedSchemaTypes + inputObjectUsedTypes
  }

  private fun Schema.Type.InputObject.usedTypes(exclude: Set<String>): Set<String> {
    val usedTypes = inputFields.map { field -> field.type.rawType.name!! }.subtract(exclude)
    val usedInputObjects = usedTypes.mapNotNull { schema[it] as? Schema.Type.InputObject }
    return usedTypes + usedInputObjects.flatMap { inputObject -> inputObject.usedTypes(exclude + usedTypes) }
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

  private fun List<Field>.mergeFields(other: List<Field>, parseContext: ParserRuleContext): List<Field> {
    val mergeFieldMap = other.groupBy { otherField ->
      find { field -> field.responseName == otherField.responseName }
    }
    return map { field ->
      field.merge(other = mergeFieldMap[field]?.firstOrNull(), parseContext = parseContext)
    } + (mergeFieldMap[null] ?: emptyList())
  }

  private fun Field.merge(other: Field?, parseContext: ParserRuleContext): Field {
    if (other == null) return this

    if (fieldName != other.fieldName) {
      throw ParseException(
          message = "Fields `$responseName` conflict because they have different schema names. Use different aliases on the fields.",
          token = parseContext.start
      )
    }

    if (type != other.type) {
      throw ParseException(
          message = "Fields `$responseName` conflict because they have different schema types. Use different aliases on the fields.",
          token = parseContext.start
      )
    }

    if (!(args ?: emptyList()).containsAll(other.args ?: emptyList())) {
      throw ParseException(
          message = "Fields `$responseName` conflict because they have different arguments. Use different aliases on the fields.",
          token = parseContext.start
      )
    }

    if (!(fields ?: emptyList()).containsAll(other.fields ?: emptyList())) {
      throw ParseException(
          message = "Fields `$responseName` conflict because they have different selection sets. Use different aliases on the fields.",
          token = parseContext.start
      )
    }

    if (!(inlineFragments ?: emptyList()).containsAll(other.inlineFragments ?: emptyList())) {
      throw ParseException(
          message = "Fields `$responseName` conflict because they have different inline fragment. Use different aliases on the fields.",
          token = parseContext.start
      )
    }

    return copy(fragmentSpreads = (fragmentSpreads ?: emptyList()).union(other.fragmentSpreads ?: emptyList()).toList())
  }

  private fun List<Operation>.checkMultipleOperationDefinitions() {
    groupBy { it.filePath.formatPackageName(dropLast = true) + it.operationName }
        .values
        .find { it.size > 1 }
        ?.last()
        ?.run {
          throw GraphQLParseException("$filePath: There can be only one operation named `$operationName`")
        }
  }

  private fun List<Fragment>.checkMultipleFragmentDefinitions() {
    groupBy { it.fragmentName }
        .values
        .find { it.size > 1 }
        ?.last()
        ?.run { throw GraphQLParseException("$filePath: There can be only one fragment named `$fragmentName`") }
  }

  private fun List<Field>.referencedFragmentNames(fragments: List<Fragment>, filePath: String): Set<String> {
    val referencedFragmentNames = flatMap { it.fragmentSpreads ?: emptyList() } +
        flatMap { it.fields?.referencedFragmentNames(fragments = fragments, filePath = filePath) ?: emptySet() } +
        flatMap { it.inlineFragments?.flatMap { it.referencedFragments(fragments = fragments, filePath = filePath) } ?: emptyList() }
    return referencedFragmentNames.toSet().flatMap { fragmentName ->
      val fragment = fragments.find { fragment -> fragment.fragmentName == fragmentName }
          ?: throw GraphQLParseException("Undefined fragment `$fragmentName`\n$filePath")
      listOf(fragmentName) + fragment.referencedFragments(fragments)
    }.toSet()
  }

  private fun Fragment.referencedFragments(fragments: List<Fragment>): Set<String> {
    return fragmentSpreads
        .flatMap { fragmentName ->
          val fragment = fragments.find { fragment -> fragment.fragmentName == fragmentName }
              ?: throw GraphQLParseException("Undefined fragment `$fragmentName`\n$filePath")

          listOf(fragmentName) + fragment.referencedFragments(fragments)
        }
        .union(fields.referencedFragmentNames(fragments = fragments, filePath = filePath!!))
        .union(inlineFragments.flatMap { it.referencedFragments(fragments = fragments, filePath = filePath) })
  }

  private fun InlineFragment.referencedFragments(fragments: List<Fragment>, filePath: String): Set<String> {
    return (fragmentSpreads ?: emptyList())
        .flatMap { fragmentName ->
          val fragment = fragments.find { fragment -> fragment.fragmentName == fragmentName }
              ?: throw GraphQLParseException("Undefined fragment `$fragmentName`\n$filePath")

          listOf(fragmentName) + fragment.referencedFragments(fragments)
        }
        .union(fields.referencedFragmentNames(fragments = fragments, filePath = filePath))
  }

  private fun Operation.checkVariableDefinitions() {
    fields.forEach { field ->
      field.checkVariableDefinitions(operation = this, filePath = filePath)
      field.fields?.forEach { it.checkVariableDefinitions(operation = this, filePath = filePath) }
    }
  }

  private fun Fragment.checkVariableDefinitions(operation: Operation) {
    try {
      fields.forEach { field ->
        field.checkVariableDefinitions(operation = operation, filePath = filePath!!)
        field.fields?.forEach { it.checkVariableDefinitions(operation = operation, filePath = filePath) }
      }
    } catch (e: ParseException) {
      throw GraphQLParseException("$filePath: ${e.message}[${operation.filePath}]")
    }
  }

  private fun Field.checkVariableDefinitions(operation: Operation, filePath: String) {
    args?.forEach { arg ->
      if (arg.value is Map<*, *> && arg.value["kind"] == "Variable") {
        val variableName = arg.value["variableName"]
        val variable = operation.variables.find { it.name == variableName } ?: throw ParseException(
            message = "Variable `$variableName` is not defined by operation `${operation.operationName}`",
            sourceLocation = arg.sourceLocation
        )

        if (!arg.type.isGraphQLTypeAssignableFrom(variable.type)) {
          throw ParseException(
              message = "Variable `$variableName` of type `${variable.type}` used in position expecting type `${arg.type}`",
              sourceLocation = arg.sourceLocation
          )
        }
      }
    }

    inlineFragments?.forEach { fragment ->
      fragment.fields.forEach { field ->
        field.checkVariableDefinitions(operation = operation, filePath = filePath)
      }
    }

    conditions?.forEach { condition ->
      val variable = operation.variables.find { it.name == condition.variableName } ?: throw ParseException(
          message = "Variable `${condition.variableName}` is not defined by operation `${operation.operationName}`",
          sourceLocation = condition.sourceLocation
      )

      val scalarType = ScalarType.forName(variable.type.removeSuffix("!"))
      if (scalarType != ScalarType.BOOLEAN) {
        throw ParseException(
            message = "Variable `${variable.name}` of type `${variable.type}` used in position expecting type `Boolean!`",
            sourceLocation = condition.sourceLocation
        )
      }
    }

    fields?.forEach { it.checkVariableDefinitions(operation = operation, filePath = filePath) }
  }

  private fun Schema.Type.isAssignableFrom(other: Schema.Type): Boolean {
    return when (this) {
      is Schema.Type.Union -> name == other.name || (possibleTypes ?: emptyList()).mapNotNull { it.rawType.name }.contains(other.name)
      is Schema.Type.Interface -> name == other.name || (possibleTypes ?: emptyList()).mapNotNull { it.rawType.name }.contains(other.name)
      is Schema.Type.Object -> name == other.name
      else -> false
    }
  }

  private fun String.isGraphQLTypeAssignableFrom(otherType: String): Boolean {
    var i = 0
    var j = 0
    do {
      when {
        this[i] == otherType[j] -> {
          i++; j++
        }
        otherType[j] == '!' -> j++
        else -> return false
      }
    } while (i < length && j < otherType.length)

    return i == length && (j == otherType.length || (otherType[j] == '!' && j == otherType.length - 1))
  }
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

private operator fun SourceLocation.Companion.invoke(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)


class ParseException(message: String, val sourceLocation: SourceLocation) : RuntimeException(message) {
  companion object {
    operator fun invoke(message: String, token: Token) = ParseException(
        message = message,
        sourceLocation = SourceLocation(token)
    )
  }
}

class GraphQLParseException(message: String) : RuntimeException(message) {
  override fun fillInStackTrace(): Throwable = this
}

class GraphQLDocumentParseException(
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
      if (parseException.sourceLocation == SourceLocation.UNKNOWN) {
        return "\nFailed to parse GraphQL file $graphQLFilePath:\n${parseException.message}"
      }

      val documentLines = document.lines()
      return "\nFailed to parse GraphQL file $graphQLFilePath (${parseException.sourceLocation.line}:" +
          "${parseException.sourceLocation.position})\n${parseException.message}" +
          "\n----------------------------------------------------\n" +
          parseException.let { error ->
            val prefix = if (error.sourceLocation.line - 2 >= 0) {
              "[${error.sourceLocation.line - 1}]:" + documentLines[error.sourceLocation.line - 2]
            } else ""
            val body = if (error.sourceLocation.line - 1 >= 0) {
              "\n[${error.sourceLocation.line}]:${documentLines[error.sourceLocation.line - 1]}\n"
            } else ""
            val postfix = if (error.sourceLocation.line < documentLines.size) {
              "[${error.sourceLocation.line + 1}]:" + documentLines[error.sourceLocation.line]
            } else ""
            "$prefix$body$postfix"
          } +
          "\n----------------------------------------------------"
    }
  }
}
