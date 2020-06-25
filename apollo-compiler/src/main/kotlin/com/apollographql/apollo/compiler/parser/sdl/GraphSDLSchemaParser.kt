package com.apollographql.apollo.compiler.parser.sdl

import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.parser.antlr.GraphSDLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphSDLParser
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
              throw GraphSdlDocumentParseException(
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
    } catch (e: GraphSdlParseException) {
      throw GraphSdlDocumentParseException(
          parseException = e,
          filePath = absolutePath
      )
    }
  }

  private fun GraphSDLParser.DocumentContext.parse(): GraphSdlSchema {
    val typeDefinitions = typeDefinition()
        ?.flatMap { ctx ->
          listOfNotNull(
              ctx.enumTypeDefinition()?.parse(),
              ctx.objectTypeDefinition()?.parse(),
              ctx.interfaceTypeDefinition()?.parse(),
              ctx.inputObjectDefinition()?.parse(),
              ctx.unionTypeDefinition()?.parse(),
              ctx.scalarTypeDefinition()?.parse()
          )
        }
        ?.associateBy { it.name }

    val schemaDefinition = schemaDefinition().firstOrNull()
    val operationRootTypes = schemaDefinition?.operationTypesDefinition().parse()
    return GraphSdlSchema(
        schema = GraphSdlSchema.Schema(
            description = schemaDefinition?.description()?.parse(),
            directives = schemaDefinition?.directives().parse(),
            queryRootOperationType = GraphSdlSchema.TypeRef.Named(operationRootTypes["query"] ?: "query"),
            mutationRootOperationType = GraphSdlSchema.TypeRef.Named(operationRootTypes["mutation"] ?: "mutation"),
            subscriptionRootOperationType = GraphSdlSchema.TypeRef.Named(operationRootTypes["subscription"] ?: "subscription")
        ),
        typeDefinitions = typeDefinitions ?: emptyMap()
    )
  }

  private fun GraphSDLParser.OperationTypesDefinitionContext?.parse(): Map<String, String> {
    return this
        ?.operationTypeDefinition()
        ?.map { it.operationType().text to it.operationType().text }
        ?.toMap()
        ?: emptyMap()
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
        directives = directives().parse()
    )
  }

  private fun GraphSDLParser.ObjectTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Object {
    return GraphSdlSchema.TypeDefinition.Object(
        name = name().text,
        description = description()?.text,
        directives = directives().parse(),
        fields = fieldsDefinition().parse(),
        interfaces = implementsIntefaces().parse()
    )
  }

  private fun GraphSDLParser.InterfaceTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Interface {
    return GraphSdlSchema.TypeDefinition.Interface(
        name = name().text,
        description = description()?.text,
        directives = directives().parse(),
        fields = fieldsDefinition().parse()
    )
  }

  private fun GraphSDLParser.InputObjectDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.InputObject {
    return GraphSdlSchema.TypeDefinition.InputObject(
        name = name().text,
        description = description()?.text,
        directives = directives().parse(),
        fields = inputValuesDefinition().parse()
    )
  }

  private fun GraphSDLParser.UnionTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Union {
    return GraphSdlSchema.TypeDefinition.Union(
        name = name().text,
        description = description()?.text,
        directives = directives().parse(),
        typeRefs = unionMemberTypes()?.unionMemberType()?.map { it.namedType().parse() } ?: emptyList()
    )
  }

  private fun GraphSDLParser.ScalarTypeDefinitionContext.parse(): GraphSdlSchema.TypeDefinition.Scalar {
    return GraphSdlSchema.TypeDefinition.Scalar(
        name = name().text,
        description = description()?.text,
        directives = directives().parse()
    )
  }

  private fun GraphSDLParser.ImplementsIntefacesContext?.parse(): List<GraphSdlSchema.TypeRef.Named> {
    return this
        ?.implementsInteface()
        ?.map { GraphSdlSchema.TypeRef.Named(it.namedType().text) }
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
        description = description()?.text,
        directives = directives().parse(),
        type = type().parse(),
        arguments = argumentsDefinition().parse()
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
        description = description()?.text,
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
        description = description()?.text,
        directives = directives().parse(),
        type = type().parse(),
        defaultValue = defaultValue()?.value()?.parse()
    )
  }

  private fun GraphSDLParser.ValueContext.parse(): Any? {
    return when {
      intValue() != null -> intValue().INT().text.toInt()
      floatValue() != null -> floatValue().FLOAT().text.toDouble()
      booleanValue() != null -> booleanValue().text == "true"
      enumValue() != null -> enumValue().name().text
      listValue() != null -> text
      objectValue() != null -> text
      stringValue() != null -> text
      nullValue() != null -> null
      else -> throw GraphSdlParseException(
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
      else -> throw GraphSdlParseException(
          message = "Illegal type reference",
          token = start
      )
    }
  }

  private fun GraphSDLParser.NamedTypeContext.parse(): GraphSdlSchema.TypeRef.Named {
    return GraphSdlSchema.TypeRef.Named(name().text)
  }

  private fun GraphSDLParser.ListTypeContext.parse(): GraphSdlSchema.TypeRef.List {
    return GraphSdlSchema.TypeRef.List(type().parse())
  }

  private fun GraphSDLParser.NonNullTypeContext.parse(): GraphSdlSchema.TypeRef.NonNull {
    return when {
      namedType() != null -> GraphSdlSchema.TypeRef.NonNull(namedType().parse())
      listType() != null -> GraphSdlSchema.TypeRef.NonNull(listType().parse())
      else -> throw GraphSdlParseException(
          message = "Illegal type reference",
          token = this.start
      )
    }
  }

  private fun GraphSDLParser.DescriptionContext?.parse(): String? {
    return this?.STRING()?.text?.removePrefix("\"")?.removeSuffix("\"")
        ?: this?.BLOCK_STRING()?.text?.removePrefix("\"\"\"\n")?.removeSuffix("\n\"\"\"")
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
        arguments = directiveArguments().parse()
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
