package com.apollographql.apollo3.ast.internal

import com.apollographql.apollo3.generated.antlr.GraphQLParser
import org.antlr.v4.runtime.Token

import com.apollographql.apollo3.ast.*

/**
 * Functions that wrap the antlr types to our own [GQLNode] values
 */
internal fun GraphQLParser.DocumentContext.toGQLDocument(filePath: String? = null) = AntlrToGQLScope(filePath).parseDocumentContext(this)

internal fun GraphQLParser.ValueContext.toGQLValue(filePath: String? = null) = AntlrToGQLScope(filePath).parseValueContext(this)

internal fun GraphQLParser.SelectionContext.toGQLSelection(filePath: String? = null) = AntlrToGQLScope(filePath).parseSelection(this)

private class AntlrToGQLScope(val filePath: String?) {
  private fun sourceLocation(token: Token) = SourceLocation(
      line = token.line,
      position = token.charPositionInLine,
      filePath = filePath
  )

  fun parseDocumentContext(documentContext: GraphQLParser.DocumentContext): GQLDocument {
    return GQLDocument(
        definitions = documentContext.definition().map { it.parse() },
        filePath = filePath
    )
  }

  fun parseValueContext(valueContext: GraphQLParser.ValueContext): GQLValue {
    return valueContext.parse()
  }

  fun parseSelection(selectionContext: GraphQLParser.SelectionContext): GQLSelection {
    return selectionContext.parse()
  }

  private fun astBuilderException(message: String, token: Token): Nothing {
    throw UnrecognizedAntlrRule(message, sourceLocation(token))
  }

  private fun GraphQLParser.DefinitionContext.parse(): GQLDefinition {
    return executableDefinition()?.parse()
        ?: typeSystemDefinition()?.parse()
        ?: typeSystemExtension()?.parse()
        ?: astBuilderException("Unrecognized definition", start)
  }

  private fun GraphQLParser.TypeSystemExtensionContext.parse(): GQLDefinition {
    return schemaExtension()?.parse()
        ?: typeExtension()?.parse()
        ?: astBuilderException("Unrecognized type system extension", start)
  }

  private fun GraphQLParser.TypeExtensionContext.parse(): GQLDefinition {
    return enumTypeExtensionDefinition()?.parse()
        ?: inputObjectTypeExtensionDefinition()?.parse()
        ?: objectTypeExtensionDefinition()?.parse()
        ?: unionTypeExtensionDefinition()?.parse()
        ?: scalarTypeExtensionDefinition()?.parse()
        ?: interfaceTypeExtensionDefinition()?.parse()
        ?: astBuilderException("Unrecognized type extension", start)
  }

  private fun GraphQLParser.InterfaceTypeExtensionDefinitionContext.parse(): GQLInterfaceTypeExtension {
    return GQLInterfaceTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name().text,
        implementsInterfaces = implementsInterfaces().parse(),
        fields = fieldsDefinition().parse()
    )
  }

  private fun GraphQLParser.ScalarTypeExtensionDefinitionContext.parse(): GQLScalarTypeExtension {
    return GQLScalarTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name().text,
        directives = directives().parse(),
    )
  }

  private fun GraphQLParser.UnionTypeExtensionDefinitionContext.parse(): GQLUnionTypeExtension {
    return GQLUnionTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name().text,
        directives = directives().parse(),
        memberTypes = unionMemberTypes().parse()
    )
  }

  private fun GraphQLParser.ObjectTypeExtensionDefinitionContext.parse(): GQLObjectTypeExtension {
    return GQLObjectTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name().text,
        directives = directives().parse(),
        implementsInterfaces = implementsInterfaces().parse(),
        fields = fieldsDefinition().parse()
    )
  }

  private fun GraphQLParser.InputObjectTypeExtensionDefinitionContext.parse(): GQLInputObjectTypeExtension {
    return GQLInputObjectTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name().text,
        directives = directives().parse(),
        inputFields = inputFieldsDefinition().parse()
    )
  }


  private fun GraphQLParser.EnumTypeExtensionDefinitionContext.parse(): GQLEnumTypeExtension {
    return GQLEnumTypeExtension(
        sourceLocation = sourceLocation(start),
        name = name().text,
        directives = directives().parse(),
        enumValues = enumValuesDefinition().parse()
    )
  }

  private fun GraphQLParser.SchemaExtensionContext.parse(): GQLSchemaExtension {
    return GQLSchemaExtension(
        sourceLocation = sourceLocation(start),
        directives = directives().parse(),
        operationTypesDefinition = operationTypesDefinition().parse()
    )
  }

  private fun GraphQLParser.TypeSystemDefinitionContext.parse(): GQLDefinition {
    return typeDefinition()?.parse()
        ?: directiveDefinition()?.parse()
        ?: typeDefinition()?.parse()
        ?: schemaDefinition()?.parse()
        ?: astBuilderException("Unrecognized executable definition", start)
  }

  private fun GraphQLParser.SchemaDefinitionContext.parse(): GQLSchemaDefinition {
    return GQLSchemaDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        directives = directives().parse(),
        rootOperationTypeDefinitions = operationTypesDefinition().parse()
    )
  }

  private fun GraphQLParser.DirectiveDefinitionContext.parse(): GQLDirectiveDefinition {
    return GQLDirectiveDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        arguments = argumentsDefinition().parse(),
        repeatable = REPEATABLE() != null,
        locations = directiveLocations().parse()
    )
  }

  private fun GraphQLParser.TypeDefinitionContext.parse(): GQLDefinition {
    return enumTypeDefinition()?.parse()
        ?: inputObjectDefinition()?.parse()
        ?: objectTypeDefinition()?.parse()
        ?: scalarTypeDefinition()?.parse()
        ?: unionTypeDefinition()?.parse()
        ?: interfaceTypeDefinition()?.parse()
        ?: astBuilderException("Unrecognized type definition", start)
  }

  private fun GraphQLParser.InterfaceTypeDefinitionContext.parse(): GQLInterfaceTypeDefinition {
    return GQLInterfaceTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        implementsInterfaces = implementsInterfaces().parse(),
        fields = fieldsDefinition().parse(),
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.DescriptionContext.parse(): String {
    return this.stringValue().parse().value
  }

  private fun GraphQLParser.UnionTypeDefinitionContext.parse(): GQLUnionTypeDefinition {
    return GQLUnionTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse(),
        memberTypes = unionMemberTypes().parse()
    )
  }

  private fun GraphQLParser.ScalarTypeDefinitionContext.parse(): GQLScalarTypeDefinition {
    return GQLScalarTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.ObjectTypeDefinitionContext.parse(): GQLObjectTypeDefinition {
    return GQLObjectTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse(),
        fields = fieldsDefinition().parse(),
        implementsInterfaces = implementsInterfaces().parse()
    )
  }

  private fun GraphQLParser.InputObjectDefinitionContext.parse(): GQLInputObjectTypeDefinition {
    return GQLInputObjectTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse(),
        inputFields = inputFieldsDefinition().parse(),
    )
  }


  private fun GraphQLParser.EnumTypeDefinitionContext.parse(): GQLEnumTypeDefinition {
    return GQLEnumTypeDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse(),
        enumValues = enumValuesDefinition().parse()
    )
  }

  private fun GraphQLParser.ExecutableDefinitionContext.parse(): GQLDefinition {
    return operationDefinition()?.parse()
        ?: fragmentDefinition()?.parse()
        ?: astBuilderException("Unrecognized executable definition", start)
  }

  private fun GraphQLParser.FragmentDefinitionContext.parse(): GQLFragmentDefinition {
    return GQLFragmentDefinition(
        sourceLocation = sourceLocation(start),
        name = fragmentName().text,
        description = description()?.parse(),
        directives = directives().parse(),
        typeCondition = typeCondition().namedType().parse(),
        selectionSet = selectionSet().parse()
    )
  }

  private fun GraphQLParser.OperationDefinitionContext.parse(): GQLOperationDefinition {
    return GQLOperationDefinition(
        sourceLocation = sourceLocation(start),
        operationType = operationType().text,
        name = name()?.text,
        description = description()?.parse(),
        variableDefinitions = variableDefinitions().parse(),
        directives = directives().parse(),
        selectionSet = selectionSet().parse()
    )
  }

  private fun GraphQLParser.SelectionContext.parse(): GQLSelection {
    return field()?.parse()
        ?: inlineFragment()?.parse()
        ?: fragmentSpread()?.parse()
        ?: astBuilderException("Unrecognized selection", start)
  }

  private fun GraphQLParser.FragmentSpreadContext.parse(): GQLFragmentSpread {
    return GQLFragmentSpread(
        // use the name for sourceLocation to be consistent with the IR, this could ultimately be removed
        sourceLocation = sourceLocation(fragmentName().start),
        name = fragmentName().text,
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.InlineFragmentContext.parse(): GQLInlineFragment {
    return GQLInlineFragment(
        sourceLocation = sourceLocation(start),
        typeCondition = typeCondition().namedType().parse(),
        directives = directives().parse(),
        selectionSet = selectionSet().parse()
    )
  }

  private fun GraphQLParser.FieldContext.parse(): GQLField {
    return GQLField(
        sourceLocation = sourceLocation(start),
        alias = alias()?.name()?.text,
        name = name().text,
        arguments = arguments()?.parse(),
        directives = directives().parse(),
        selectionSet = selectionSet()?.parse()
    )
  }

  private fun GraphQLParser.FieldDefinitionContext.parse(): GQLFieldDefinition {
    return GQLFieldDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        arguments = argumentsDefinition().parse(),
        type = type().parse(),
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.DirectivesContext?.parse() = this?.directive()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.VariableDefinitionsContext?.parse() = this?.variableDefinition()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.ImplementsInterfacesContext?.parse() = this?.implementsInterface()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.FieldsDefinitionContext?.parse() = this?.fieldDefinition()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.ArgumentsDefinitionContext?.parse() = this?.inputValueDefinition()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.UnionMemberTypesContext?.parse() = this?.namedType()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.InputFieldsDefinitionContext?.parse() = this?.inputValueDefinition()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.EnumValuesDefinitionContext?.parse() = this?.enumValueDefinition()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.OperationTypesDefinitionContext?.parse() = this?.operationTypeDefinition()?.map { it.parse() } ?: emptyList()
  private fun GraphQLParser.DirectiveLocationsContext?.parse() = this?.directiveLocation()?.map { it.parse() } ?: emptyList()

  private fun GraphQLParser.SelectionSetContext.parse() = GQLSelectionSet(this.selection()?.map { it.parse() }
      ?: emptyList(), sourceLocation(start))

  private fun GraphQLParser.ArgumentsContext.parse() = GQLArguments(this.argument()?.map { it.parse() }
      ?: emptyList(), sourceLocation(start))

  private fun GraphQLParser.OperationTypeDefinitionContext.parse(): GQLOperationTypeDefinition {
    return GQLOperationTypeDefinition(
        sourceLocation = sourceLocation(start),
        operationType = operationType().text,
        namedType = namedType().text
    )
  }

  private fun GraphQLParser.DirectiveLocationContext.parse(): GQLDirectiveLocation {
    return GQLDirectiveLocation.values().firstOrNull { it.name == name().text }
        ?: astBuilderException("Unrecognized directive location '${name().text}", start)
  }

  private fun GraphQLParser.EnumValueDefinitionContext.parse(): GQLEnumValueDefinition {
    return GQLEnumValueDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse(),
    )
  }

  private fun GraphQLParser.InputValueDefinitionContext.parse(): GQLInputValueDefinition {
    return GQLInputValueDefinition(
        sourceLocation = sourceLocation(start),
        description = description()?.parse(),
        name = name().text,
        directives = directives().parse(),
        type = type().parse(),
        defaultValue = defaultValue()?.parse()
    )
  }

  private fun GraphQLParser.ImplementsInterfaceContext.parse(): String {
    return namedType().text
  }

  private fun GraphQLParser.DirectiveContext.parse(): GQLDirective {
    return GQLDirective(
        sourceLocation = sourceLocation(start),
        name = name().text,
        arguments = arguments()?.parse()
    )
  }

  private fun GraphQLParser.ArgumentContext.parse(): GQLArgument {
    return GQLArgument(
        sourceLocation = sourceLocation(start),
        name = name().text,
        value = value().parse()
    )
  }

  private fun GraphQLParser.VariableDefinitionContext.parse(): GQLVariableDefinition {
    return GQLVariableDefinition(
        sourceLocation = sourceLocation(start),
        name = variable().name().text,
        type = type().parse(),
        defaultValue = defaultValue()?.parse(),
        directives = directives().parse()
    )
  }

  private fun GraphQLParser.DefaultValueContext.parse(): GQLValue {
    return value().parse()
  }

  private fun GraphQLParser.ValueContext.parse(): GQLValue {
    return variable()?.parse()
        ?: intValue()?.parse()
        ?: floatValue()?.parse()
        ?: stringValue()?.parse()
        ?: booleanValue()?.parse()
        ?: enumValue()?.parse()
        ?: listValue()?.parse()
        ?: objectValue()?.parse()
        ?: nullValue()?.parse()
        ?: astBuilderException("Unrecognized value", start)
  }

  private fun GraphQLParser.NullValueContext.parse() = GQLNullValue(sourceLocation(start))

  private fun GraphQLParser.ObjectValueContext.parse(): GQLObjectValue {
    return GQLObjectValue(
        sourceLocation = sourceLocation(start),
        fields = objectField().map {
          GQLObjectField(
              sourceLocation = sourceLocation(start),
              name = it.name().text,
              value = it.value().parse())
        }
    )
  }

  private fun GraphQLParser.ListValueContext.parse(): GQLListValue {
    return GQLListValue(
        sourceLocation = sourceLocation(start),
        values = value().map { it.parse() }
    )
  }

  private fun GraphQLParser.VariableContext.parse(): GQLVariableValue {
    return GQLVariableValue(
        sourceLocation = sourceLocation(start),
        name = name().text
    )
  }

  private fun GraphQLParser.IntValueContext.parse() = GQLIntValue(
      sourceLocation = sourceLocation(start),
      value = text.toInt()
  )

  private fun GraphQLParser.FloatValueContext.parse() = GQLFloatValue(
      sourceLocation = sourceLocation(start),
      value = text.toDouble()
  )

  private fun GraphQLParser.BooleanValueContext.parse() = GQLBooleanValue(
      sourceLocation = sourceLocation(start),
      value = text.toBoolean()
  )

  private fun GraphQLParser.EnumValueContext.parse() = GQLEnumValue(
      sourceLocation = sourceLocation(start),
      value = name().text
  )

  private fun GraphQLParser.StringValueContext.parse(): GQLStringValue {
    return GQLStringValue(
        sourceLocation = sourceLocation(start),
        value = STRING()?.text?.removePrefix("\"")?.removeSuffix("\"")?.decodeAsGraphQLSingleQuoted()
            ?: BLOCK_STRING()?.text?.removePrefix("\"\"\"")?.removeSuffix("\"\"\"")?.decodeAsGraphQLTripleQuoted()
            ?: astBuilderException("Unrecognized string", start)
    )
  }

  private fun GraphQLParser.TypeContext.parse(): GQLType {
    return namedType()?.parse()
        ?: nonNullType()?.parse()
        ?: listType()?.parse()
        ?: astBuilderException("Unrecognized type", start)
  }

  private fun GraphQLParser.ListTypeContext.parse(): GQLListType {
    return GQLListType(
        sourceLocation = sourceLocation(start),
        type = type().parse()
    )
  }

  private fun GraphQLParser.NamedTypeContext.parse(): GQLNamedType {
    return GQLNamedType(
        sourceLocation = sourceLocation(start),
        name = text
    )
  }

  private fun GraphQLParser.NonNullTypeContext.parse(): GQLNonNullType {
    return GQLNonNullType(
        sourceLocation = sourceLocation(start),
        type = namedType()?.parse()
            ?: listType()?.parse()
            ?: astBuilderException("Unrecognized non-null type", start)
    )
  }
}

