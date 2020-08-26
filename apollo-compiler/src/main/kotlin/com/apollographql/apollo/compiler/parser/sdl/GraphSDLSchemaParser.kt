package com.apollographql.apollo.compiler.parser.sdl

import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.parser.antlr.GraphSDLLexer
import com.apollographql.apollo.compiler.parser.antlr.GraphSDLParser
import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.sdl.GraphSDLSchemaParser.parse
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

internal object GraphSDLSchemaParser {
  /**
   * Built in scalar and introspection types from:
   * - https://spec.graphql.org/June2018/#sec-Scalars
   * - https://spec.graphql.org/June2018/#sec-Schema-Introspection
   */
  private val builtInTypes = """
""${'"'}
The `Int` scalar type represents non-fractional signed whole numeric values. Int can represent values between -(2^31) and 2^31 - 1.
""${'"'}
scalar Int

""${'"'}
The `Float` scalar type represents signed double-precision fractional values as specified by [IEEE 754](http://en.wikipedia.org/wiki/IEEE_floating_point).
""${'"'}
scalar Float

""${'"'}
The `String` scalar type represents textual data, represented as UTF-8 character sequences. The String type is most often used by GraphQL to represent free-form human-readable text.
""${'"'}
scalar String

""${'"'}
The `Boolean` scalar type represents `true` or `false`.
""${'"'}
scalar Boolean

""${'"'}
The `ID` scalar type represents a unique identifier, often used to refetch an object or as key for a cache. The ID type appears in a JSON response as a String; however, it is not intended to be human-readable. When expected as an input type, any string (such as `"4"`) or integer (such as `4`) input value will be accepted as an ID.
""${'"'}
scalar ID

""${'"'}
A Directive provides a way to describe alternate runtime execution and type validation behavior in a GraphQL document.

In some cases, you need to provide options to alter GraphQL's execution behavior in ways field arguments will not suffice, such as conditionally including or skipping a field. Directives provide this by describing additional information to the executor.
""${'"'}
type __Directive {
  name: String!
  description: String
  locations: [__DirectiveLocation!]!
  args: [__InputValue!]!
}

""${'"'}
A Directive can be adjacent to many parts of the GraphQL language, a __DirectiveLocation describes one such possible adjacencies.
""${'"'}
enum __DirectiveLocation {
  ""${'"'}Location adjacent to a query operation.""${'"'}
  QUERY

  ""${'"'}Location adjacent to a mutation operation.""${'"'}
  MUTATION

  ""${'"'}Location adjacent to a subscription operation.""${'"'}
  SUBSCRIPTION

  ""${'"'}Location adjacent to a field.""${'"'}
  FIELD

  ""${'"'}Location adjacent to a fragment definition.""${'"'}
  FRAGMENT_DEFINITION

  ""${'"'}Location adjacent to a fragment spread.""${'"'}
  FRAGMENT_SPREAD

  ""${'"'}Location adjacent to an inline fragment.""${'"'}
  INLINE_FRAGMENT

  ""${'"'}Location adjacent to a schema definition.""${'"'}
  SCHEMA

  ""${'"'}Location adjacent to a scalar definition.""${'"'}
  SCALAR

  ""${'"'}Location adjacent to an object type definition.""${'"'}
  OBJECT

  ""${'"'}Location adjacent to a field definition.""${'"'}
  FIELD_DEFINITION

  ""${'"'}Location adjacent to an argument definition.""${'"'}
  ARGUMENT_DEFINITION

  ""${'"'}Location adjacent to an interface definition.""${'"'}
  INTERFACE

  ""${'"'}Location adjacent to a union definition.""${'"'}
  UNION

  ""${'"'}Location adjacent to an enum definition.""${'"'}
  ENUM

  ""${'"'}Location adjacent to an enum value definition.""${'"'}
  ENUM_VALUE

  ""${'"'}Location adjacent to an input object type definition.""${'"'}
  INPUT_OBJECT

  ""${'"'}Location adjacent to an input object field definition.""${'"'}
  INPUT_FIELD_DEFINITION
}

""${'"'}
One possible value for a given Enum. Enum values are unique values, not a placeholder for a string or numeric value. However an Enum value is returned in a JSON response as a string.
""${'"'}
type __EnumValue {
  name: String!
  description: String
  isDeprecated: Boolean!
  deprecationReason: String
}

""${'"'}
Object and Interface types are described by a list of Fields, each of which has a name, potentially a list of arguments, and a return type.
""${'"'}
type __Field {
  name: String!
  description: String
  args: [__InputValue!]!
  type: __Type!
  isDeprecated: Boolean!
  deprecationReason: String
}

""${'"'}
Arguments provided to Fields or Directives and the input fields of an InputObject are represented as Input Values which describe their type and optionally a default value.
""${'"'}
type __InputValue {
  name: String!
  description: String
  type: __Type!

  ""${'"'}
  A GraphQL-formatted string representing the default value for this input value.
  ""${'"'}
  defaultValue: String
}

""${'"'}
A GraphQL Schema defines the capabilities of a GraphQL server. It exposes all available types and directives on the server, as well as the entry points for query, mutation, and subscription operations.
""${'"'}
type __Schema {
  ""${'"'}A list of all types supported by this server.""${'"'}
  types: [__Type!]!

  ""${'"'}The type that query operations will be rooted at.""${'"'}
  queryType: __Type!

  ""${'"'}
  If this server supports mutation, the type that mutation operations will be rooted at.
  ""${'"'}
  mutationType: __Type
  ""${'"'}
  If this server supports subscription, the type that subscription operations will be rooted at.
  ""${'"'}
  subscriptionType: __Type
  ""${'"'}A list of all directives supported by this server.""${'"'}
  directives: [__Directive!]!
}
""${'"'}
The fundamental unit of any GraphQL Schema is the type. There are many kinds of types in GraphQL as represented by the `__TypeKind` enum.

Depending on the kind of a type, certain fields describe information about that type. Scalar types provide no information beyond a name and description, while Enum types provide their values. Object and Interface types provide the fields they describe. Abstract types, Union and Interface, provide the Object types possible at runtime. List and NonNull types compose other types.
""${'"'}
type __Type {
  kind: __TypeKind!
  name: String
  description: String
  fields(includeDeprecated: Boolean = false): [__Field!]
  interfaces: [__Type!]
  possibleTypes: [__Type!]
  enumValues(includeDeprecated: Boolean = false): [__EnumValue!]
  inputFields: [__InputValue!]
  ofType: __Type
}
""${'"'}An enum describing what kind of type a given `__Type` is.""${'"'}
enum __TypeKind {
  ""${'"'}Indicates this type is a scalar.""${'"'}
  SCALAR
  ""${'"'}
  Indicates this type is an object. `fields` and `interfaces` are valid fields.
  ""${'"'}
  OBJECT
  ""${'"'}
  Indicates this type is an interface. `fields` and `possibleTypes` are valid fields.
  ""${'"'}
  INTERFACE
  ""${'"'}Indicates this type is a union. `possibleTypes` is a valid field.""${'"'}
  UNION
  ""${'"'}Indicates this type is an enum. `enumValues` is a valid field.""${'"'}
  ENUM
  ""${'"'}
  Indicates this type is an input object. `inputFields` is a valid field.
  ""${'"'}
  INPUT_OBJECT
  ""${'"'}Indicates this type is a list. `ofType` is a valid field.""${'"'}
  LIST
  ""${'"'}Indicates this type is a non-null. `ofType` is a valid field.""${'"'}
  NON_NULL
}
  """.trimIndent()

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

  private fun builtInTypeDefinitions() = GraphSDLParser(CommonTokenStream(GraphSDLLexer(ANTLRInputStream(builtInTypes)))).document()
        .typeDefinition().parse()

  private fun GraphSDLParser.DocumentContext.parse(): GraphSdlSchema {
    val typeDefinitions = typeDefinition()?.parse()
        ?.plus(builtInTypeDefinitions())
        ?.associateBy { it.name }

    val schemaDefinition = schemaDefinition().firstOrNull()
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
        typeDefinitions = typeDefinitions ?: emptyMap()
    )
  }

  private fun GraphSDLParser.OperationTypesDefinitionContext?.parse(): Map<String, String> {
    return this
        ?.operationTypeDefinition()
        ?.map { it.operationType().text to it.namedType().text }
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
        typeRefs = unionMemberTypes()?.unionMemberType()?.map { it.namedType().parse() } ?: emptyList()
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
        defaultValue = defaultValue()?.value()?.parse()
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

private operator fun SourceLocation.Companion.invoke(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)
