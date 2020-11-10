package com.apollographql.apollo.compiler.parser.graphql

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.ir.Argument
import com.apollographql.apollo.compiler.ir.Condition
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.ir.FragmentRef
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.ir.Operation
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.ir.Variable
import com.apollographql.apollo.compiler.parser.antlr.GraphQLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.graphql.GraphQLDocumentSourceBuilder.graphQLDocumentSource
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.asGraphQLType
import com.apollographql.apollo.compiler.parser.introspection.isAssignableFrom
import com.apollographql.apollo.compiler.parser.introspection.possibleTypes
import com.apollographql.apollo.compiler.parser.introspection.resolveType
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.File
import java.io.IOException

class GraphQLDocumentParser(
    private val schema: IntrospectionSchema,
    private val packageNameProvider: PackageNameProvider,
    ) {
  fun parse(graphQLFiles: Collection<File>): DocumentParseResult {
    return graphQLFiles.fold(DocumentParseResult()) { acc, graphQLFile ->
      val result = graphQLFile.parse()
      DocumentParseResult(
          operations = acc.operations + result.operations,
          fragments = acc.fragments + result.fragments,
          usedTypes = acc.usedTypes.union(result.usedTypes)
      )
    }
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
          throw DocumentParseException(
              message = "Unsupported token `${(offendingSymbol as? Token)?.text ?: offendingSymbol.toString()}`",
              filePath = absolutePath,
              sourceLocation = SourceLocation(
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
          .parse(tokenStream, absolutePath)
    } catch (e: ParseException) {
      throw DocumentParseException(
          parseException = e,
          filePath = absolutePath
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

  private fun GraphQLParser.DocumentContext.parse(tokenStream: CommonTokenStream, graphQLFilePath: String): DocumentParseResult {
    val fragments = definition().mapNotNull { it.fragmentDefinition()?.parse(tokenStream, graphQLFilePath) }
    val operations = definition().mapNotNull { ctx ->
      ctx.operationDefinition()?.parse(tokenStream, graphQLFilePath)
    }
    return DocumentParseResult(
        operations = operations.map { it.result },
        fragments = fragments.map { it.result },
        usedTypes = fragments.flatMap { it.usedTypes }.union(operations.flatMap { it.usedTypes })
    )
  }

  private fun GraphQLParser.OperationDefinitionContext.parse(tokenStream: CommonTokenStream, graphQLFilePath: String): ParseResult<Operation> {
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

    val commentTokens = tokenStream.getHiddenTokensToLeft(start.tokenIndex, 2) ?: emptyList()
    val description = commentTokens.joinToString(separator = "\n") { token ->
      token.text.trim().removePrefix("#")
    }
    val operation = Operation(
        operationName = operationName,
        packageName = packageNameProvider.operationPackageName(graphQLFilePath),
        operationType = operationType,
        description = description,
        variables = variables.result,
        source = graphQLDocumentSource,
        sourceWithFragments = graphQLDocumentSource,
        fields = fields.result.filterNot { it.responseName == Field.TYPE_NAME_FIELD.responseName },
        fragments = selectionSet().fragmentRefs(),
        fragmentsReferenced = emptyList(),
        filePath = graphQLFilePath
    ).also { it.validateArguments(schema = schema) }

    return ParseResult(
        result = operation,
        usedTypes = variables.usedTypes + fields.usedTypes
    )
  }

  private fun GraphQLParser.OperationTypeContext.schemaType(): IntrospectionSchema.Type {
    val operationRoot = when (text.toLowerCase()) {
      "query" -> schema.queryType
      "mutation" -> schema.mutationType
      "subscription" -> schema.subscriptionType
      else -> throw ParseException(
          message = "Unknown operation type `$text`",
          token = start
      )
    }

    val schemaType = schema[operationRoot] ?: throw ParseException(
        message = "Can't resolve root for `$text` operation type",
        token = start
    )

    return if (operationRoot == schema.queryType && schemaType is IntrospectionSchema.Type.Object) {
      val schemaField = IntrospectionSchema.Field(
          name = "__schema",
          description = null,
          deprecationReason = null,
          type = IntrospectionSchema.TypeRef(
              kind = IntrospectionSchema.Kind.NON_NULL,
              name = null,
              ofType = IntrospectionSchema.TypeRef(
                  kind = IntrospectionSchema.Kind.OBJECT,
                  name = "__Schema",
                  ofType = null
              )
          )
      )
      val typeField = IntrospectionSchema.Field(
          name = "__type",
          description = null,
          deprecationReason = null,
          type = IntrospectionSchema.TypeRef(
              kind = IntrospectionSchema.Kind.OBJECT,
              name = "__Type",
              ofType = null
          ),
          args = listOf(IntrospectionSchema.Field.Argument(
              name = "name",
              description = null,
              deprecationReason = null,
              type = IntrospectionSchema.TypeRef(
                  kind = IntrospectionSchema.Kind.NON_NULL,
                  name = null,
                  ofType = IntrospectionSchema.TypeRef(
                      kind = IntrospectionSchema.Kind.SCALAR,
                      name = "String",
                      ofType = null
                  )
              ),
              defaultValue = null
          ))
      )
      schemaType.copy(fields = schemaType.fields?.plus(schemaField)?.plus(typeField))
    } else {
      schemaType
    }
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
            // parse doesn't work with nullable types (see https://github.com/apollographql/apollo-android/issues/2742)
            // defaultValue = defaultValue()?.value()?.parse(schema.resolveType(type)),
            sourceLocation = SourceLocation(variable().NAME().symbol)
        ),
        usedTypes = setOf(schemaType.name)
    )
  }

  private fun GraphQLParser.SelectionSetContext?.parse(schemaType: IntrospectionSchema.Type): ParseResult<List<Field>> {
    val hasInlineFragments = this?.selection()?.find { it.inlineFragment() != null } != null
    val hasFragments = this?.selection()?.find { it.fragmentSpread() != null } != null
    val hasFields = this?.selection()?.find { it.field() != null } != null

    return this
        ?.selection()
        ?.mapNotNull { ctx -> ctx.field()?.parse(schemaType) }
        ?.flatten()
        ?.let { (fields, usedTypes) ->
          val reconciledFields = fields.groupBy { it.responseName }.map { (_, fields) ->
            fields.fold<Field, Field?>(null) { first, second ->
              first?.merge(second) ?: second
            }!!
          }
          ParseResult(
              result = reconciledFields,
              usedTypes = usedTypes
          )
        }
        ?.let { (fields, usedTypes) ->
          val withTypenameField = (hasFields || hasInlineFragments || hasFragments) &&
              fields.find { it.responseName == Field.TYPE_NAME_FIELD.responseName } == null
          ParseResult(
              result = (if (withTypenameField) listOf(Field.TYPE_NAME_FIELD) else emptyList()) + fields,
              usedTypes = usedTypes
          )
        }
        ?: ParseResult(result = if (hasFragments) listOf(Field.TYPE_NAME_FIELD) else emptyList(), usedTypes = emptySet())
  }

  private fun GraphQLParser.FieldContext.parse(schemaType: IntrospectionSchema.Type): ParseResult<Field> {
    val responseName = fieldName().alias()?.NAME(0)?.text ?: fieldName().NAME().text
    val fieldName = fieldName().alias()?.NAME(1)?.text ?: fieldName().NAME().text
    if (responseName == Field.TYPE_NAME_FIELD.responseName) {
      return ParseResult(result = Field.TYPE_NAME_FIELD, usedTypes = emptySet())
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

    if (fields.result.isEmpty() && (schemaFieldType.kind == IntrospectionSchema.Kind.INTERFACE ||
            schemaFieldType.kind == IntrospectionSchema.Kind.OBJECT ||
            schemaFieldType.kind == IntrospectionSchema.Kind.UNION)) {
      throw ParseException(
          message = "Field `$fieldName` of type `${schemaType.name}` must have a selection of sub-fields",
          token = start
      )
    }

    val fragmentRefs = selectionSet().fragmentRefs()
    val inlineFragmentsResult = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.inlineFragment()?.parse(parentSchemaType = schemaFieldType, parentFields = fields)
    }?.flatten() ?: ParseResult(result = emptyList())

    val (inlineFragments, inlineFragmentsToMerge) = inlineFragmentsResult.result.partition {
      it.typeCondition != schemaFieldType.name || it.conditions.isNotEmpty()
    }
    val inlineFragmentFieldsToMerge = inlineFragmentsToMerge
        .flatMap { it.fields }
        .filter { it.responseName != Field.TYPE_NAME_FIELD.responseName }
    val inlineFragmentRefsToMerge = inlineFragmentsToMerge.flatMap { it.fragments }
    val mergedFields = fields.result.mergeFields(others = inlineFragmentFieldsToMerge)

    val conditions = directives().parse()
    return ParseResult(
        result = Field(
            responseName = responseName,
            fieldName = fieldName,
            type = schemaField.type.asGraphQLType(),
            typeDescription = schemaFieldType.description ?: "",
            args = arguments.result,
            isConditional = conditions.isNotEmpty(),
            fields = mergedFields,
            fragmentRefs = fragmentRefs.union(inlineFragmentRefsToMerge).toList(),
            inlineFragments = inlineFragments.map {
              it.copy(
                  fields = it.fields.mergeFields(others = mergedFields)
              )
            },
            description = schemaField.description?.trim() ?: "",
            deprecationReason = schemaField.deprecationReason,
            conditions = conditions,
            sourceLocation = SourceLocation(start)
        ),
        usedTypes = setOf(schemaField.type.rawType.name!!)
            .union(arguments.usedTypes)
            .union(fields.usedTypes)
            .union(inlineFragmentsResult.usedTypes)
    )
  }

  private fun GraphQLParser.SelectionSetContext?.fragmentRefs(): List<FragmentRef> {
    return this
        ?.selection()
        ?.mapNotNull { ctx -> ctx.fragmentSpread() }
        ?.map { fragmentSpread ->
          FragmentRef(
              name = fragmentSpread.fragmentName().NAME().text,
              conditions = fragmentSpread.directives().parse(),
              sourceLocation = SourceLocation(fragmentSpread.fragmentName().NAME().symbol)
          )
        }
        ?: emptyList()
  }

  private fun GraphQLParser.DirectivesContext?.parse(): List<Condition> {
    return this?.directive()?.mapNotNull { ctx ->
      val name = ctx.NAME().text
      val arguments = ctx.arguments()

      if (name != "skip" && name != "include" ) {
        // we only support skip and include directives for now
        return@mapNotNull null
      }

      if (arguments.argument().size != 1) {
        // skip and include should only have a single argument
        return@mapNotNull null
      }

      val argument = arguments.argument().first()
      if (argument.NAME().text != "if" || argument.valueOrVariable().variable() == null) {
        // The argument should be named "if"
        // Not 100% why we ignore non-variable arguments
        return@mapNotNull null
      }
      Condition(
          kind = "BooleanCondition",
          variableName = argument.valueOrVariable().variable().NAME().text,
          inverted = name == "skip",
          sourceLocation = SourceLocation(argument.start)
      )
    } ?: emptyList()
  }

  private fun GraphQLParser.ArgumentsContext?.parse(schemaField: IntrospectionSchema.Field): ParseResult<List<Argument>> {
    return this
        ?.argument()
        ?.map { ctx -> ctx.parse(schemaField) }
        ?.flatten()
        ?: ParseResult(result = emptyList(), usedTypes = emptySet())
  }

  private fun GraphQLParser.ArgumentContext.parse(schemaField: IntrospectionSchema.Field): ParseResult<Argument> {
    val name = NAME().text
    val schemaArgument = schemaField.args.find { it.name == name } ?: throw ParseException(
        message = "Unknown argument `$name` on field `${schemaField.name}`",
        token = NAME().symbol
    )

    val type = schemaArgument.type.asGraphQLType()
    val value = valueOrVariable().variable()?.let { ctx ->
      mapOf(
          "kind" to "Variable",
          "variableName" to ctx.NAME().text
      )
    } ?: valueOrVariable().value().parse(schemaArgument.type)
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
      parentSchemaType: IntrospectionSchema.Type,
      parentFields: ParseResult<List<Field>>

  ): ParseResult<InlineFragment> {
    val typeCondition = typeCondition().typeName().NAME().text
    val schemaType = schema[typeCondition] ?: throw ParseException(
        message = "Unknown type`$typeCondition}`",
        token = typeCondition().typeName().start
    )

    if (!parentSchemaType.isAssignableFrom(other = schemaType, schema = schema)) {
      throw ParseException(
          message = "Fragment cannot be spread here as result can never be of type `$typeCondition`",
          token = typeCondition().typeName().start
      )
    }

    val decoratedParentFields = parentFields.let { (parentFields, usedTypes) ->
      // if inline fragment conditional type contains the same field as parent type
      // carry over meta info such as: `description`, `deprecationReason`
      val decoratedFields = parentFields.map { parentField ->
        when (schemaType) {
          is IntrospectionSchema.Type.Interface -> schemaType.fields?.find { it.name == parentField.fieldName }
          is IntrospectionSchema.Type.Object -> schemaType.fields?.find { it.name == parentField.fieldName }
          is IntrospectionSchema.Type.Union -> schemaType.fields?.find { it.name == parentField.fieldName }
          else -> null
        }?.let { field ->
          parentField.copy(
              description = field.description ?: parentField.description,
              deprecationReason = field.deprecationReason
          )
        } ?: parentField
      }
      ParseResult(
          result = decoratedFields,
          usedTypes = usedTypes
      )
    }

    val fields = decoratedParentFields.plus(selectionSet().parse(schemaType)) { left, right -> left.union(right) }
    if (fields.result.isEmpty()) {
      throw ParseException(
          message = "Inline fragment `$typeCondition` must have a selection of sub-fields",
          token = typeCondition().typeName().NAME().symbol
      )
    }

    val inlineFragmentsResult = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.inlineFragment()?.parse(parentSchemaType = schemaType, parentFields = fields)
    }?.flatten() ?: ParseResult(result = emptyList())

    val possibleTypes = schemaType.possibleTypes(schema).toList()
    return ParseResult(
        result = InlineFragment(
            typeCondition = typeCondition,
            possibleTypes = possibleTypes,
            description = schemaType.description ?: "",
            fields = fields.result,
            inlineFragments = inlineFragmentsResult.result,
            fragments = selectionSet().fragmentRefs(),
            sourceLocation = SourceLocation(start),
            conditions = directives().parse()
        ),
        usedTypes = setOf(typeCondition).union(fields.usedTypes)
    )
  }

  private fun GraphQLParser.FragmentDefinitionContext.parse(tokenStream: CommonTokenStream, graphQLFilePath: String): ParseResult<Fragment> {
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

    val possibleTypes = schemaType.possibleTypes(schema)
    val fields = selectionSet().parse(schemaType)
    if (fields.result.isEmpty()) {
      throw ParseException(
          message = "Fragment `$fragmentName` must have a selection of sub-fields",
          token = fragmentName().NAME().symbol
      )
    }

    val fragmentRefs = selectionSet().fragmentRefs()

    val inlineFragments = selectionSet()?.selection()?.mapNotNull { ctx ->
      ctx.inlineFragment()?.parse(parentSchemaType = schemaType, parentFields = fields)
    }?.flatten() ?: ParseResult(result = emptyList())

    val mergeInlineFragmentFields = inlineFragments.result
        .filter { it.typeCondition == typeCondition }
        .flatMap { it.fields }
        .filter { it.responseName != Field.TYPE_NAME_FIELD.responseName }
    val mergeInlineFragmentRefs = inlineFragments.result
        .filter { it.typeCondition == typeCondition }
        .flatMap { it.fragments }

    val commentTokens = tokenStream.getHiddenTokensToLeft(start.tokenIndex, 2) ?: emptyList()
    val description = commentTokens.joinToString(separator = "\n") { token ->
      token.text.trim().removePrefix("#")
    }
    return ParseResult(
        result = Fragment(
            fragmentName = fragmentName,
            typeCondition = typeCondition,
            source = graphQLDocumentSource,
            description = description,
            possibleTypes = possibleTypes.toList(),
            fields = fields.result.mergeFields(mergeInlineFragmentFields),
            fragmentRefs = fragmentRefs.union(mergeInlineFragmentRefs).toList(),
            inlineFragments = inlineFragments.result.filter { it.typeCondition != typeCondition },
            filePath = graphQLFilePath,
            sourceLocation = SourceLocation(start),
            packageName = packageNameProvider.fragmentPackageName(graphQLFilePath)
        ),
        usedTypes = setOf(typeCondition)
            .union(fields.usedTypes)
            .union(inlineFragments.usedTypes)
    )
  }

  private fun GraphQLParser.ValueContext.parse(schemaTypeRef: IntrospectionSchema.TypeRef): Any? {
    return when (schemaTypeRef.kind) {
      IntrospectionSchema.Kind.ENUM -> text.toString().trim()
      IntrospectionSchema.Kind.INTERFACE, IntrospectionSchema.Kind.OBJECT, IntrospectionSchema.Kind.INPUT_OBJECT, IntrospectionSchema.Kind.UNION -> {
        val inlineInputType = when (this) {
          is GraphQLParser.InlineInputTypeValueContext -> inlineInputType()
          is GraphQLParser.LiteralValueContext -> if (text.toLowerCase() == "null") null else throw ParseException(
              message = "Can't parse `Object` value `${this.text}`",
              token = start
          )
          else -> throw throw ParseException(
              message = "Can't parse `Object` value `${this.text}`",
              token = start
          )
        }
        when {
          inlineInputType == null -> null
          inlineInputType.emptyMap() != null -> emptyMap<String, Any?>()
          else -> inlineInputType.inlineInputTypeField().map { field ->
            val name = field.NAME().text
            val schemaFieldType = schema[schemaTypeRef.name]!!.let { schemaType ->
              when (schemaType) {
                is IntrospectionSchema.Type.InputObject -> schemaType.lookupField(fieldName = name, token = field.NAME().symbol).type
                else -> schemaType.lookupField(fieldName = name, token = field.NAME().symbol).type
              }
            }
            val variableValue = field.valueOrVariable().variable()?.NAME()?.text
            val value = field.valueOrVariable().value()?.parse(schemaFieldType)
            name to when {
              variableValue != null -> mapOf(
                  "kind" to "Variable",
                  "variableName" to variableValue
              )
              else -> value
            }
          }.toMap()
        }
      }
      IntrospectionSchema.Kind.SCALAR -> when (ScalarType.forName(schemaTypeRef.name ?: "")) {
        ScalarType.INT -> text.trim().toIntOrNull() ?: throw ParseException(
            message = "Can't parse `Int` value",
            token = start
        )
        ScalarType.BOOLEAN -> text.trim().toLowerCase() == "true"
        ScalarType.FLOAT -> text.trim().toDoubleOrNull() ?: throw ParseException(
            message = "Can't parse `Float` value",
            token = start
        )
        else -> text.toString().replace("\"", "")
      }
      IntrospectionSchema.Kind.NON_NULL -> parse(schemaTypeRef.ofType!!)
      IntrospectionSchema.Kind.LIST -> {
        val arrayValueType = (this as? GraphQLParser.ArrayValueContext)?.arrayValueType() ?: throw ParseException(
            message = "Can't parse `Array` value, expected array",
            token = start
        )
        when {
          arrayValueType.emptyArray() != null -> emptyList<Any?>()
          else -> arrayValueType.valueOrVariable().map { valueOrVariable ->
            valueOrVariable.variable()?.let { variable ->
              mapOf(
                  "kind" to "Variable",
                  "variableName" to variable.NAME().text
              )
            } ?: valueOrVariable.value().parse(schemaTypeRef.ofType!!)
          }
        }
      }
    }
  }

  private fun IntrospectionSchema.Type.lookupField(fieldName: String, token: Token): IntrospectionSchema.Field {
    val field = when (this) {
      is IntrospectionSchema.Type.Interface -> fields?.find { it.name == fieldName }
      is IntrospectionSchema.Type.Object -> fields?.find { it.name == fieldName }
      is IntrospectionSchema.Type.Union -> fields?.find { it.name == fieldName }
      else -> throw ParseException(
          message = "Can't query `$fieldName` on type `$name`. `$name` is not one of the expected types: `INTERFACE`, `OBJECT`, `UNION`.",
          token = token
      )
    }
    return field ?: throw ParseException(
        message = "Can't query `$fieldName` on type `$name`",
        token = token
    )
  }

  private fun IntrospectionSchema.Type.InputObject.lookupField(fieldName: String, token: Token): IntrospectionSchema.InputField {
    return inputFields.find { it.name == fieldName } ?: throw ParseException(
        message = "Can't query `$fieldName` on type `$name`",
        token = token
    )
  }

  private fun List<Field>.union(other: List<Field>): List<Field> {
    val fieldNames = map { it.responseName + ":" + it.fieldName }
    return map { targetField ->
      val targetFieldName = targetField.responseName + ":" + targetField.fieldName
      targetField.copy(
          fields = targetField.fields.union(
              other.find { otherField ->
                otherField.responseName + ":" + otherField.fieldName == targetFieldName
              }?.fields ?: emptyList()
          ),
          inlineFragments = targetField.inlineFragments + (other.find { otherField ->
            otherField.responseName + ":" + otherField.fieldName == targetFieldName
          }?.inlineFragments ?: emptyList())
      )
    } + other.filter { (it.responseName + ":" + it.fieldName) !in fieldNames }
  }

  private fun <T> List<ParseResult<T>>.flatten() = ParseResult(
      result = map { (result, _) -> result },
      usedTypes = flatMap { (_, usedTypes) -> usedTypes }.toSet()
  )
}

data class DocumentParseResult(
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
