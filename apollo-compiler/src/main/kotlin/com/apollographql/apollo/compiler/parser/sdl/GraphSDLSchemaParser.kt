package com.apollographql.apollo.compiler.parser.sdl

import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.parser.antlr.GraphSDLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphSDLParser
import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.File
import java.io.IOException
import java.util.Locale

object GraphSDLSchemaParser {
  fun File.parse(): GraphSdlSchema {
    val document = try {
      readText()
    } catch (e: IOException) {
      throw RuntimeException("Failed to read GraphQL SDL schema file `$this`", e)
    }
    return document.parse(absolutePath)
  }

  fun String.parse(absolutePath: String = "(source)"): GraphSdlSchema {
    val document = this

    val tokenStream = GraphSDLLexer(ANTLRInputStream(document))
        .apply { removeErrorListeners() }
        .let { CommonTokenStream(it) }

    val parser = GraphSDLParser(tokenStream).apply {
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
              throw DocumentParseException(
                  message = "Unsupported token `${(offendingSymbol as? Token)?.text ?: offendingSymbol.toString()}`",
                  filePath = absolutePath,
                  sourceLocation = SourceLocation(
                      line = line,
                      position = position
                  )
              )
            }
          }
      )
    }

    try {
      return parser.document().parse()
    } catch (e: ParseException) {
      throw DocumentParseException(
          parseException = e,
          filePath = absolutePath
      )
    }
  }

  private fun List<GraphSDLParser.TypeDefinitionContext>.parse() = flatMap { ctx ->
    listOfNotNull(
        ctx.enumTypeDefinition()?.parse(),
        ctx.objectTypeDefinition()?.parse(),
        ctx.interfaceTypeDefinition()?.parse(),
        ctx.inputObjectDefinition()?.parse(),
        ctx.unionTypeDefinition()?.parse(),
        ctx.scalarTypeDefinition()?.parse()
    )
  }

  private fun builtInTypeDefinitions() = javaClass.getResourceAsStream("/builtins.sdl").use { inputStream ->
    GraphSDLParser(CommonTokenStream(GraphSDLLexer(ANTLRInputStream(inputStream)))).document()
        .typeDefinition().parse()
  }

  private fun GraphSDLParser.DocumentContext.parse(): GraphSdlSchema {
    val typeDefinitions = typeDefinition()?.parse()
        ?.plus(builtInTypeDefinitions())
        ?.associateBy { it.name }
        ?.let {
          typeDefinition().mapNotNull { it.typeSystemExtension() }.parse(it)
        }

    val schemaDefinition = schemaDefinition().firstOrNull()
    val operationRootTypes = schemaDefinition?.operationTypesDefinition().parse()
    return GraphSdlSchema(
        schema = GraphSdlSchema.Schema(
            description = schemaDefinition?.description()?.parse(),
            directives = schemaDefinition?.directives().parse(),
            queryRootOperationType = rootOperationType(operationRootTypes, "query", typeDefinitions) ?: throw IllegalStateException("No query root operation type found"),
            mutationRootOperationType = rootOperationType(operationRootTypes, "mutation", typeDefinitions),
            subscriptionRootOperationType = rootOperationType(operationRootTypes, "subscription", typeDefinitions)),
        typeDefinitions = typeDefinitions ?: emptyMap()
    )
  }

  private fun rootOperationType(operationRootTypes: Map<String, String>?, operationType: String, typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>?): String? {
    var rootOperationType = operationRootTypes?.get(operationType)
    if (rootOperationType != null) {
      check(typeDefinitions?.get(rootOperationType) is GraphSdlSchema.TypeDefinition.Object) {
        "ApolloGraphQL: schema defines '$operationType': '$rootOperationType' but no matching object type definition found"
      }
      return rootOperationType
    }

    // https://spec.graphql.org/June2018/#sec-Root-Operation-Types
    // Default rootOperationTypes are "Query", "Mutation", "Subscription"
    rootOperationType = operationType.capitalize()
    return if (typeDefinitions?.get(rootOperationType) is GraphSdlSchema.TypeDefinition.Object) {
      // The type is present, use it
      rootOperationType
    } else {
      null
    }
  }
  private fun List<GraphSDLParser.TypeSystemExtensionContext>.parse(
      typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>
  ): Map<String, GraphSdlSchema.TypeDefinition> {
    return fold(typeDefinitions) { acc, item ->
      return with(item.typeExtension()) {
        scalarTypeExtensionDefinition()?.parse(acc)
            ?: enumTypeExtensionDefinition()?.parse(acc)
            ?: objectTypeExtensionDefinition()?.parse(acc)
            ?: interfaceTypeExtensionDefinition()?.parse(acc)
            ?: unionTypeExtensionDefinition()?.parse(acc)
            ?: inputObjectTypeExtensionDefinition()?.parse(acc)
            ?: acc
      }
    }
  }

  private fun GraphSDLParser.ScalarTypeExtensionDefinitionContext.parse(typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>): Map<String, GraphSdlSchema.TypeDefinition> {
    val name = name().text
    val scalar = typeDefinitions.get(name)
        ?: throw ParseException(
            message = "Cannot add scalar type extension on unknown scalar `$name`",
            token = start
        )
    if (scalar !is GraphSdlSchema.TypeDefinition.Scalar) {
      throw ParseException(
          message = "Cannot add scalar extension on non-scalar type `$name`",
          token = start
      )
    }

    return typeDefinitions.toMutableMap().apply {
      put(name().text, scalar.copy(
          directives = scalar.directives.mergeDirectives(directives().parse()),
      ))
    }
  }

  private fun GraphSDLParser.ObjectTypeExtensionDefinitionContext.parse(typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>): Map<String, GraphSdlSchema.TypeDefinition> {
    val name = name().text
    val objectType = typeDefinitions.get(name)
        ?: throw ParseException(
            message = "Cannot add object type extension on unknown object type `$name`",
            token = start
        )
    if (objectType !is GraphSdlSchema.TypeDefinition.Object) {
      throw ParseException(
          message = "Cannot add object extension on non-object type `$name`",
          token = start
      )
    }

    return typeDefinitions.toMutableMap().apply {
      put(name, objectType.copy(
          directives = objectType.directives.mergeDirectives(directives().parse()),
          interfaces = objectType.interfaces.mergeTypeRefs(implementsInterfaces().parse()),
          fields = objectType.fields.mergeFields(fieldsDefinition().parse()),
      ))
    }
  }

  private fun GraphSDLParser.InterfaceTypeExtensionDefinitionContext.parse(typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>): Map<String, GraphSdlSchema.TypeDefinition> {
    val name = name().text
    val interfaceType = typeDefinitions.get(name)
        ?: throw ParseException(
            message = "Cannot add interface type extension on unknown interface `$name`",
            token = start
        )
    if (interfaceType !is GraphSdlSchema.TypeDefinition.Interface) {
      throw ParseException(
          message = "Cannot add interface extension on non-interface type `$name`",
          token = start
      )
    }

    return typeDefinitions.toMutableMap().apply {
      put(name, interfaceType.copy(
          directives = interfaceType.directives.mergeDirectives(directives().parse()),
          fields = interfaceType.fields.mergeFields(fieldsDefinition().parse()),
      ))
    }
  }

  private fun GraphSDLParser.UnionTypeExtensionDefinitionContext.parse(typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>): Map<String, GraphSdlSchema.TypeDefinition> {
    val name = name().text
    val union = typeDefinitions.get(name)
        ?: throw ParseException(
            message = "Cannot add union type extension on unknown union `$name`",
            token = start
        )
    if (union !is GraphSdlSchema.TypeDefinition.Union) {
      throw ParseException(
          message = "Cannot add union extension on non-union type `$name`",
          token = start
      )
    }

    return typeDefinitions.toMutableMap().apply {
      put(name, union.copy(
          directives = union.directives.mergeDirectives(directives().parse()),
          typeRefs = union.typeRefs.mergeTypeRefs(unionMemberTypes().namedType().map { it.parse() })
      ))
    }
  }

  private fun GraphSDLParser.EnumTypeExtensionDefinitionContext.parse(typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>): Map<String, GraphSdlSchema.TypeDefinition> {
    val name = name().text
    val enum = typeDefinitions.get(name)
        ?: throw ParseException(
            message = "Cannot add enum extension on unknown enum `$name`",
            token = start
        )
    if (enum !is GraphSdlSchema.TypeDefinition.Enum) {
      throw ParseException(
          message = "Cannot add enum extension on non-enum type `$name`",
          token = start
      )
    }

    return typeDefinitions.toMutableMap().apply {
      put(name, enum.copy(
          directives = enum.directives.mergeDirectives(directives().parse()),
          enumValues = enum.enumValues.mergeEnumValues(enumValuesDefinition().parse())
      ))
    }
  }

  private fun GraphSDLParser.InputObjectTypeExtensionDefinitionContext.parse(typeDefinitions: Map<String, GraphSdlSchema.TypeDefinition>): Map<String, GraphSdlSchema.TypeDefinition> {
    val name = name().text
    val inputObjectType = typeDefinitions.get(name)
        ?: throw ParseException(
            message = "Cannot add enum extension on unknown enum `$name`",
            token = start
        )
    if (inputObjectType !is GraphSdlSchema.TypeDefinition.InputObject) {
      throw ParseException(
          message = "Cannot add enum extension on non-enum type `$name`",
          token = start
      )
    }

    return typeDefinitions.toMutableMap().apply {
      put(name, inputObjectType.copy(
          directives = inputObjectType.directives.mergeDirectives(directives().parse()),
          fields = inputObjectType.fields.mergeInputFields(inputValuesDefinition().parse())
      ))
    }
  }

  private fun List<GraphSdlSchema.Directive>.mergeDirectives(newDirectives: List<GraphSdlSchema.Directive>): List<GraphSdlSchema.Directive> {
    newDirectives.forEach { newDirective ->
      if (find { it.name == newDirective.name } != null) {
        throw ParseException(
            message = "Cannot add already existing directive `${newDirective.name}`",
            sourceLocation = newDirective.sourceLocation
        )
      }
    }

    return this + newDirectives
  }

  private fun List<GraphSdlSchema.TypeRef.Named>.mergeTypeRefs(newTypeRefs: List<GraphSdlSchema.TypeRef.Named>): List<GraphSdlSchema.TypeRef.Named> {
    newTypeRefs.forEach { newTypeRef ->
      if (find { it.typeName == newTypeRef.typeName } != null) {
        throw ParseException(
            message = "Cannot add already existing type `${newTypeRef.typeName}`",
            sourceLocation = newTypeRef.sourceLocation
        )
      }
    }

    return this + newTypeRefs
  }

  private fun List<GraphSdlSchema.TypeDefinition.Field>.mergeFields(newInterfaces: List<GraphSdlSchema.TypeDefinition.Field>): List<GraphSdlSchema.TypeDefinition.Field> {
    newInterfaces.forEach { field ->
      if (find { it.name == field.name } != null) {
        throw ParseException(
            message = "Cannot add already existing field `${field.name}`",
            sourceLocation = field.sourceLocation
        )
      }
    }

    return this + newInterfaces
  }

  private fun List<GraphSdlSchema.TypeDefinition.Enum.Value>.mergeEnumValues(newEnumValues: List<GraphSdlSchema.TypeDefinition.Enum.Value>): List<GraphSdlSchema.TypeDefinition.Enum.Value> {
    newEnumValues.forEach { newEnumValue ->
      if (find { it.name == newEnumValue.name } != null) {
        throw ParseException(
            message = "Cannot add already existing enum value `${newEnumValue.name}`",
            sourceLocation = newEnumValue.sourceLocation
        )
      }
    }

    return this + newEnumValues
  }

  private fun List<GraphSdlSchema.TypeDefinition.InputField>.mergeInputFields(newInputFields: List<GraphSdlSchema.TypeDefinition.InputField>): List<GraphSdlSchema.TypeDefinition.InputField> {
    newInputFields.forEach { newInputField ->
      if (find { it.name == newInputField.name } != null) {
        throw ParseException(
            message = "Cannot add already existing enum value `${newInputField.name}`",
            sourceLocation = newInputField.sourceLocation
        )
      }
    }

    return this + newInputFields
  }

  private fun GraphSDLParser.OperationTypesDefinitionContext?.parse(): Map<String, String>? {
    return this
        ?.operationTypeDefinition()
        ?.map { it.operationType().text to it.namedType().text }
        ?.toMap()
  }

  private fun GraphSDLParser.EnumTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Enum {
    return GraphSdlSchema.TypeDefinition.Enum(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        enumValues = enumValuesDefinition().parse()
    )
  }

  private fun GraphSDLParser.EnumValuesDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.Enum.Value> {
    return this
        ?.enumValueDefinition()
        ?.map { it.parse() }
        ?: emptyList();
  }

  private fun GraphSDLParser.EnumValueDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Enum.Value {
    return GraphSdlSchema.TypeDefinition.Enum.Value(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        sourceLocation = SourceLocation(start)
    )
  }

  private fun GraphSDLParser.ObjectTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Object {
    return GraphSdlSchema.TypeDefinition.Object(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        fields = fieldsDefinition().parse(),
        interfaces = implementsInterfaces().parse()
    )
  }

  private fun GraphSDLParser.InterfaceTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Interface {
    return GraphSdlSchema.TypeDefinition.Interface(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        fields = fieldsDefinition().parse()
    )
  }

  private fun GraphSDLParser.InputObjectDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.InputObject {
    return GraphSdlSchema.TypeDefinition.InputObject(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        fields = inputValuesDefinition().parse()
    )
  }

  private fun GraphSDLParser.UnionTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Union {
    return GraphSdlSchema.TypeDefinition.Union(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        typeRefs = unionMemberTypes()?.namedType()?.map { it.parse() } ?: emptyList()
    )
  }

  private fun GraphSDLParser.ScalarTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Scalar {
    return GraphSdlSchema.TypeDefinition.Scalar(
        name = name().text,
        description = description().parse(),
        directives = directives().parse()
    )
  }

  private fun GraphSDLParser.ImplementsInterfacesContext?.parse(): List<GraphSdlSchema.TypeRef.Named> {
    return this
        ?.implementsInterface()
        ?.map { GraphSdlSchema.TypeRef.Named(it.namedType().text, SourceLocation(start)) }
        ?: emptyList()
  }

  private fun GraphSDLParser.FieldsDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.Field> {
    return this
        ?.fieldDefinition()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphSDLParser.FieldDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Field {
    return GraphSdlSchema.TypeDefinition.Field(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        type = type().parse(),
        arguments = argumentsDefinition().parse(),
        sourceLocation = SourceLocation(start)
    )
  }

  private fun GraphSDLParser.ArgumentsDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.Field.Argument> {
    return this
        ?.argumentDefinition()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphSDLParser.ArgumentDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Field.Argument {
    return GraphSdlSchema.TypeDefinition.Field.Argument(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        type = type().parse(),
        defaultValue = defaultValue()?.value()?.parse()
    )
  }

  private fun GraphSDLParser.InputValuesDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.InputField> {
    return this
        ?.inputValueDefinition()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphSDLParser.InputValueDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.InputField {
    return GraphSdlSchema.TypeDefinition.InputField(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        type = type().parse(),
        defaultValue = defaultValue()?.value()?.parse(),
        sourceLocation = SourceLocation(start)
    )
  }

  private fun GraphSDLParser.ValueContext.parse(): Any? {
    return when {
      intValue() != null -> intValue().INT().text.toInt()
      floatValue() != null -> floatValue().FLOAT().text.toDouble()
      booleanValue() != null -> booleanValue().text == "true"
      enumValue() != null -> enumValue().name().text
      listValue() != null -> listValue().value().map { it.parse() }
      objectValue() != null -> text
      stringValue() != null -> text.removePrefix("\"").removeSuffix("\"")
      nullValue() != null -> null
      else -> throw ParseException(
          message = "Illegal default value `$text`",
          token = start
      )
    }
  }

  private fun GraphSDLParser.TypeContext.parse(): GraphSdlSchema.TypeRef {
    return when {
      namedType() != null -> namedType().parse()
      listType() != null -> listType().parse()
      nonNullType() != null -> nonNullType().parse()
      else -> throw ParseException(
          message = "Illegal type reference",
          token = start
      )
    }
  }

  private fun GraphSDLParser.NamedTypeContext.parse(): GraphSdlSchema.TypeRef.Named {
    return GraphSdlSchema.TypeRef.Named(name().text, sourceLocation = SourceLocation(start))
  }

  private fun GraphSDLParser.ListTypeContext.parse(): GraphSdlSchema.TypeRef.List {
    return GraphSdlSchema.TypeRef.List(type().parse())
  }

  private fun GraphSDLParser.NonNullTypeContext.parse(): GraphSdlSchema.TypeRef.NonNull {
    return when {
      namedType() != null -> GraphSdlSchema.TypeRef.NonNull(namedType().parse())
      listType() != null -> GraphSdlSchema.TypeRef.NonNull(listType().parse())
      else -> throw ParseException(
          message = "Illegal type reference",
          token = this.start
      )
    }
  }

  private fun GraphSDLParser.DescriptionContext?.parse(): String {
    /**
     * Block strings should strip their leading spaces.
     * See https://spec.graphql.org/June2018/#sec-String-Value
     */

    return this?.STRING()?.text?.removePrefix("\"")?.removePrefix("\n")?.removeSuffix("\"")?.removeSuffix("\n")
        ?: this?.BLOCK_STRING()?.text?.removePrefix("\"\"\"")?.removeSuffix("\"\"\"")?.trimIndent()
        ?: ""
  }

  private fun GraphSDLParser.DirectivesContext?.parse(): List<GraphSdlSchema.Directive> {
    return this
        ?.directive()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphSDLParser.DirectiveContext.parse(): GraphSdlSchema.Directive {
    return GraphSdlSchema.Directive(
        name = name().text,
        arguments = directiveArguments().parse(),
        sourceLocation = SourceLocation(start)
    )
  }

  private fun GraphSDLParser.DirectiveArgumentsContext?.parse(): Map<String, String> {
    return this
        ?.directiveArgument()
        ?.map { it.name().text.toLowerCase(Locale.ENGLISH) to it.value().text }
        ?.toMap()
        ?: emptyMap()
  }
}

private operator fun SourceLocation.Companion.invoke(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)
