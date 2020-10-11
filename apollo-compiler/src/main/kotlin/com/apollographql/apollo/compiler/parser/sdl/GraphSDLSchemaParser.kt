package com.apollographql.apollo.compiler.parser.sdl

import com.apollographql.apollo.compiler.ir.SourceLocation
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

    val tokenStream = GraphQLLexer(CharStreams.fromString(document))
        .apply { removeErrorListeners() }
        .let { CommonTokenStream(it) }

    val parser = GraphQLParser(tokenStream).apply {
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

  private fun List<GraphQLParser.TypeDefinitionContext>.parse() = flatMap { ctx ->
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
    GraphQLParser(CommonTokenStream(GraphQLLexer(CharStreams.fromStream(inputStream)))).document()
        .definition().mapNotNull { it.typeSystemDefinition()?.typeDefinition() }.parse()
  }

  private fun GraphQLParser.DocumentContext.parse(): GraphSdlSchema {
    val executableDefinition = definition().firstOrNull { it.executableDefinition() != null }
    if (executableDefinition != null) {
      throw ParseException(
          message = "Found an executable definition. Schemas should not contain operations or fragments.",
          token = executableDefinition.start
      )
    }
    val typeDefinitions = definition().mapNotNull { it.typeSystemDefinition()?.typeDefinition() }.parse()
        .plus(builtInTypeDefinitions())
        .associateBy { it.name }

    val schemaDefinition = definition().mapNotNull { it.typeSystemDefinition()?.schemaDefinition() }.firstOrNull()
    val operationRootTypes = schemaDefinition?.operationTypesDefinition().parse()
    return GraphSdlSchema(
        schema = GraphSdlSchema.Schema(
            description = schemaDefinition?.description()?.parse(),
            directives = schemaDefinition?.directives().parse(),
            queryRootOperationType = GraphSdlSchema.TypeRef.Named(operationRootTypes["query"] ?: "Query", SourceLocation(start)),
            mutationRootOperationType = GraphSdlSchema.TypeRef.Named(operationRootTypes["mutation"] ?: "Mutation", SourceLocation(start)),
            subscriptionRootOperationType = GraphSdlSchema.TypeRef.Named(
                operationRootTypes["subscription"] ?: "Subscription", SourceLocation(start)
            )
        ),
        typeDefinitions = typeDefinitions
    )
  }

  private fun GraphQLParser.OperationTypesDefinitionContext?.parse(): Map<String, String> {
    return this
        ?.operationTypeDefinition()
        ?.map { it.operationType().text to it.namedType().text }
        ?.toMap()
        ?: emptyMap()
  }

  private fun GraphQLParser.EnumTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Enum {
    return GraphSdlSchema.TypeDefinition.Enum(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        enumValues = enumValuesDefinition().parse()
    )
  }

  private fun GraphQLParser.EnumValuesDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.Enum.Value> {
    return this
        ?.enumValueDefinition()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphQLParser.EnumValueDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Enum.Value {
    return GraphSdlSchema.TypeDefinition.Enum.Value(
        name = name().text,
        description = description().parse(),
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.ObjectTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Object {
    return GraphSdlSchema.TypeDefinition.Object(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        fields = fieldsDefinition().parse(),
        interfaces = implementsInterfaces().parse()
    )
  }

  private fun GraphQLParser.InterfaceTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Interface {
    return GraphSdlSchema.TypeDefinition.Interface(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        fields = fieldsDefinition().parse()
    )
  }

  private fun GraphQLParser.InputObjectDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.InputObject {
    return GraphSdlSchema.TypeDefinition.InputObject(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        fields = inputFieldsDefinition().parse()
    )
  }

  private fun GraphQLParser.UnionTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Union {
    return GraphSdlSchema.TypeDefinition.Union(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        typeRefs = unionMemberTypes()?.namedType()?.map { it.parse() } ?: emptyList()
    )
  }

  private fun GraphQLParser.ScalarTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Scalar {
    return GraphSdlSchema.TypeDefinition.Scalar(
        name = name().text,
        description = description().parse(),
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.ImplementsInterfacesContext?.parse(): List<GraphSdlSchema.TypeRef.Named> {
    return this
        ?.implementsInterface()
        ?.map { GraphSdlSchema.TypeRef.Named(it.namedType().text, SourceLocation(start)) }
        ?: emptyList()
  }

  private fun GraphQLParser.FieldsDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.Field> {
    return this
        ?.fieldDefinition()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphQLParser.FieldDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Field {
    return GraphSdlSchema.TypeDefinition.Field(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        type = type().parse(),
        arguments = argumentsDefinition().parse()
    )
  }

  private fun GraphQLParser.ArgumentsDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.Field.Argument> {
    return this
        ?.inputValueDefinition()
        ?.map { it.parseToArgument() }
        ?: emptyList()
  }

  private fun GraphQLParser.InputValueDefinitionContext.parseToArgument(): GraphSdlSchema.TypeDefinition.Field.Argument {
    return GraphSdlSchema.TypeDefinition.Field.Argument(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        type = type().parse(),
        defaultValue = defaultValue()?.value()?.parse()
    )
  }

  private fun GraphQLParser.InputFieldsDefinitionContext?.parse(): List<GraphSdlSchema.TypeDefinition.InputField> {
    return this
        ?.inputValueDefinition()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphQLParser.InputValueDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.InputField {
    return GraphSdlSchema.TypeDefinition.InputField(
        name = name().text,
        description = description().parse(),
        directives = directives().parse(),
        type = type().parse(),
        defaultValue = defaultValue()?.value()?.parse()
    )
  }

  private fun GraphQLParser.ValueContext.parse(): Any? {
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

  private fun GraphQLParser.TypeContext.parse(): GraphSdlSchema.TypeRef {
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

  private fun GraphQLParser.NamedTypeContext.parse(): GraphSdlSchema.TypeRef.Named {
    return GraphSdlSchema.TypeRef.Named(name().text, sourceLocation = SourceLocation(start))
  }

  private fun GraphQLParser.ListTypeContext.parse(): GraphSdlSchema.TypeRef.List {
    return GraphSdlSchema.TypeRef.List(type().parse())
  }

  private fun GraphQLParser.NonNullTypeContext.parse(): GraphSdlSchema.TypeRef.NonNull {
    return when {
      namedType() != null -> GraphSdlSchema.TypeRef.NonNull(namedType().parse())
      listType() != null -> GraphSdlSchema.TypeRef.NonNull(listType().parse())
      else -> throw ParseException(
          message = "Illegal type reference",
          token = this.start
      )
    }
  }

  private fun GraphQLParser.DescriptionContext?.parse(): String {
    /**
     * Block strings should strip their leading spaces.
     * See https://spec.graphql.org/June2018/#sec-String-Value
     */

    return this?.STRING()?.text?.removePrefix("\"")?.removePrefix("\n")?.removeSuffix("\"")?.removeSuffix("\n")
        ?: this?.BLOCK_STRING()?.text?.removePrefix("\"\"\"")?.removeSuffix("\"\"\"")?.trimIndent()
        ?: ""
  }

  private fun GraphQLParser.DirectivesContext?.parse(): List<GraphSdlSchema.Directive> {
    return this
        ?.directive()
        ?.map { it.parse() }
        ?: emptyList()
  }

  private fun GraphQLParser.DirectiveContext.parse(): GraphSdlSchema.Directive {
    return GraphSdlSchema.Directive(
        name = name().text,
        arguments = arguments().parse()
    )
  }

  private fun GraphQLParser.ArgumentsContext?.parse(): Map<String, String> {
    return this
        ?.argument()
        ?.map { it.name().text.toLowerCase(Locale.ENGLISH) to it.value().text }
        ?.toMap()
        ?: emptyMap()
  }
}

private operator fun SourceLocation.Companion.invoke(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)
