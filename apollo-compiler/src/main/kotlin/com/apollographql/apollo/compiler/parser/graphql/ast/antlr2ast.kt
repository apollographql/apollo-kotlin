package com.apollographql.apollo.compiler.parser.graphql.ast

import com.apollographql.apollo.compiler.ir.SourceLocation
import com.apollographql.apollo.compiler.parser.antlr.GraphQLParser
import com.apollographql.apollo.compiler.parser.error.ParseException
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode


private fun SourceLocation(token: Token) = SourceLocation(
    line = token.line,
    position = token.charPositionInLine
)


fun GraphQLParser.DocumentContext.parse(): GQLDocument {
  return GQLDocument(
      definitions = definition().map { it.parse() }
  )
}

private fun GraphQLParser.DefinitionContext.parse(): GQLDefinition {
  return executableDefinition()?.parse()
      ?: typeSystemDefinition()?.parse()
      ?: typeSystemExtension()?.parse()
      ?: throw ParseException("Unrecognized definition", start)
}

private fun GraphQLParser.TypeSystemExtensionContext.parse(): GQLDefinition {
  return schemaExtension()?.parse()
      ?: typeExtension()?.parse()
      ?: throw ParseException("Unrecognized type system extension", start)
}

private fun GraphQLParser.TypeExtensionContext.parse(): GQLDefinition {
  return enumTypeExtensionDefinition()?.parse()
      ?: inputObjectTypeExtensionDefinition()?.parse()
      ?: objectTypeExtensionDefinition()?.parse()
      ?: unionTypeExtensionDefinition()?.parse()
      ?: scalarTypeExtensionDefinition()?.parse()
      ?: interfaceTypeExtensionDefinition()?.parse()
      ?: throw ParseException("Unrecognized type extension", start)
}

private fun GraphQLParser.InterfaceTypeExtensionDefinitionContext.parse(): GQLInterfaceTypeExtension {
  return GQLInterfaceTypeExtension(
      sourceLocation = SourceLocation(start),
      name = name().text,
      fields = fieldsDefinition().parse()
  )
}

private fun GraphQLParser.ScalarTypeExtensionDefinitionContext.parse(): GQLScalarTypeExtension {
  return GQLScalarTypeExtension(
      sourceLocation = SourceLocation(start),
      name = name().text,
      directives = directives().parse(),
  )
}

private fun GraphQLParser.UnionTypeExtensionDefinitionContext.parse(): GQLUnionTypeExtension {
  return GQLUnionTypeExtension(
      sourceLocation = SourceLocation(start),
      name = name().text,
      directives = directives().parse(),
      memberTypes = unionMemberTypes().parse()
  )
}

private fun GraphQLParser.ObjectTypeExtensionDefinitionContext.parse(): GQLObjectTypeExtension {
  return GQLObjectTypeExtension(
      sourceLocation = SourceLocation(start),
      name = name().text,
      directives = directives().parse(),
      fields = fieldsDefinition().parse()
  )
}

private fun GraphQLParser.InputObjectTypeExtensionDefinitionContext.parse(): GQLInputObjectTypeExtension {
  return GQLInputObjectTypeExtension(
      sourceLocation = SourceLocation(start),
      name = name().text,
      directives = directives().parse(),
      inputFields = inputFieldsDefinition().parse()
  )
}


private fun GraphQLParser.EnumTypeExtensionDefinitionContext.parse(): GQLEnumTypeExtension {
  return GQLEnumTypeExtension(
      sourceLocation = SourceLocation(start),
      name = name().text,
      directives = directives().parse(),
      enumValues = enumValuesDefinition().parse()
  )
}

private fun GraphQLParser.SchemaExtensionContext.parse(): GQLSchemaExtension {
  return GQLSchemaExtension(
      sourceLocation = SourceLocation(start),
      directives = directives().parse(),
      operationTypesDefinition = operationTypesDefinition().parse()
  )
}

private fun GraphQLParser.TypeSystemDefinitionContext.parse(): GQLDefinition {
  return typeDefinition()?.parse()
      ?: directiveDefinition()?.parse()
      ?: typeDefinition()?.parse()
      ?: schemaDefinition()?.parse()
      ?: throw ParseException("Unrecognized executable definition", start)
}

private fun GraphQLParser.SchemaDefinitionContext.parse(): GQLSchemaDefinition {
  return GQLSchemaDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      directives = directives().parse(),
      rootOperationTypeDefinitions = operationTypesDefinition().parse()
  )
}

private fun GraphQLParser.DirectiveDefinitionContext.parse(): GQLDirectiveDefinition {
  return GQLDirectiveDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
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
      ?: throw ParseException("Unrecognized type definition", start)
}

private fun GraphQLParser.InterfaceTypeDefinitionContext.parse(): GQLInterfaceTypeDefinition {
  return GQLInterfaceTypeDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      implementsInterfaces = implementsInterfaces().parse(),
      fields = fieldsDefinition().parse(),
      directives = directives().parse()
  )
}

private fun GraphQLParser.DescriptionContext?.parse(): String {
  return this?.STRING()?.withoutQuotes()
      ?: this?.BLOCK_STRING()?.withoutBlockQuotes()
      ?: ""
}

private fun GraphQLParser.UnionTypeDefinitionContext.parse(): GQLUnionTypeDefinition {
  return GQLUnionTypeDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      directives = directives().parse(),
      memberTypes = unionMemberTypes().parse()
  )
}

private fun GraphQLParser.ScalarTypeDefinitionContext.parse(): GQLScalarTypeDefinition {
  return GQLScalarTypeDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      directives = directives().parse()
  )
}

private fun GraphQLParser.ObjectTypeDefinitionContext.parse(): GQLObjectTypeDefinition {
  return GQLObjectTypeDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      directives = directives().parse(),
      fields = fieldsDefinition().parse(),
      implementsInterfaces = implementsInterfaces().parse()
  )
}

private fun GraphQLParser.InputObjectDefinitionContext.parse(): GQLInputObjectTypeDefinition {
  return GQLInputObjectTypeDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      directives = directives().parse(),
      inputFields = inputFieldsDefinition().parse(),
  )
}


private fun GraphQLParser.EnumTypeDefinitionContext.parse(): GQLEnumTypeDefinition {
  return GQLEnumTypeDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      directives = directives().parse(),
      enumValues = enumValuesDefinition().parse()
  )
}

private fun GraphQLParser.ExecutableDefinitionContext.parse(): GQLDefinition {
  return operationDefinition()?.parse()
      ?: fragmentDefinition()?.parse()
      ?: throw ParseException("Unrecognized executable definition", start)
}

private fun GraphQLParser.FragmentDefinitionContext.parse(): GQLFragmentDefinition {
  return GQLFragmentDefinition(
      sourceLocation = SourceLocation(start),
      name = fragmentName().text,
      directives = directives().parse(),
      typeCondition = typeCondition().namedType().parse(),
      selections = selectionSet().parse()
  )
}

private fun GraphQLParser.OperationDefinitionContext.parse(): GQLOperationDefinition {
  return GQLOperationDefinition(
      sourceLocation = SourceLocation(start),
      operationType = operationType().text,
      name = name()?.text,
      variableDefinitions = variableDefinitions().parse(),
      directives = directives().parse(),
      selections = selectionSet().parse()
  )
}

private fun GraphQLParser.SelectionContext.parse(): GQLSelection {
  return field()?.parse()
      ?: inlineFragment()?.parse()
      ?: fragmentSpread()?.parse()
      ?: throw ParseException("Unrecognized selection", start)
}

private fun GraphQLParser.FragmentSpreadContext.parse(): GQLFragmentSpread {
  return GQLFragmentSpread(
      sourceLocation = SourceLocation(start),
      name = fragmentName().text,
      directives = directives().parse()
  )
}

private fun GraphQLParser.InlineFragmentContext.parse(): GQLInlineFragment {
  return GQLInlineFragment(
      sourceLocation = SourceLocation(start),
      typeCondition = typeCondition().namedType().parse(),
      directives = directives().parse(),
      selections = selectionSet().parse()
  )
}

private fun GraphQLParser.FieldContext.parse(): GQLField {
  return GQLField(
      sourceLocation = SourceLocation(start),
      alias = alias()?.name()?.text,
      name = name().text,
      arguments = arguments().parse(),
      directives = directives().parse(),
      selections = selectionSet().parse()
  )
}

private fun GraphQLParser.FieldDefinitionContext.parse(): GQLFieldDefinition {
  return GQLFieldDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      arguments = argumentsDefinition().parse(),
      type = type().parse(),
      directives = directives().parse()
  )
}

private fun GraphQLParser.ArgumentsContext?.parse() = this?.argument()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.DirectivesContext?.parse() = this?.directive()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.SelectionSetContext?.parse() = this?.selection()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.VariableDefinitionsContext?.parse() = this?.variableDefinition()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.ImplementsInterfacesContext?.parse() = this?.implementsInterface()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.FieldsDefinitionContext?.parse() = this?.fieldDefinition()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.ArgumentsDefinitionContext?.parse() = this?.inputValueDefinition()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.UnionMemberTypesContext?.parse() = this?.namedType()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.InputFieldsDefinitionContext?.parse() = this?.inputValueDefinition()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.EnumValuesDefinitionContext?.parse() = this?.enumValueDefinition()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.OperationTypesDefinitionContext?.parse() = this?.operationTypeDefinition()?.map { it.parse() } ?: emptyList()
private fun GraphQLParser.DirectiveLocationsContext?.parse() = this?.directiveLocation()?.map { it.parse() } ?: emptyList()

private fun GraphQLParser.OperationTypeDefinitionContext.parse(): GQLOperationTypeDefinition {
  return GQLOperationTypeDefinition(
      sourceLocation = SourceLocation(start),
      operationType = operationType().text,
      namedType = namedType().text
  )
}

private fun GraphQLParser.DirectiveLocationContext.parse(): GQLDirectiveLocation {
  return GQLDirectiveLocation.values().firstOrNull { it.name == name().text } ?: throw ParseException("Unrecognized directive location '${name().text}", start)
}

private fun GraphQLParser.EnumValueDefinitionContext.parse(): GQLEnumValueDefinition {
  return GQLEnumValueDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
      name = name().text,
      directives = directives().parse(),
  )
}

private fun GraphQLParser.InputValueDefinitionContext.parse(): GQLInputValueDefinition {
  return GQLInputValueDefinition(
      sourceLocation = SourceLocation(start),
      description = description().parse(),
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
      sourceLocation = SourceLocation(start),
      name = name().text,
      arguments = arguments().parse()
  )
}

private fun GraphQLParser.ArgumentContext.parse(): GQLArgument {
  return GQLArgument(
      sourceLocation = SourceLocation(start),
      name = name().text,
      value = value().parse()
  )
}

private fun GraphQLParser.VariableDefinitionContext.parse(): GQLVariableDefinition {
  return GQLVariableDefinition(
      sourceLocation = SourceLocation(start),
      name = variable().name().text,
      type = type().parse(),
      defaultValue = defaultValue().parse(),
      // TODO("support directives")
      // directives = directives().parse()
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
      ?: throw ParseException("Unrecognized value", start)
}

private fun GraphQLParser.NullValueContext.parse() = GQLNullValue(SourceLocation(start))

private fun GraphQLParser.ObjectValueContext.parse(): GQLObjectValue {
  return GQLObjectValue(
      sourceLocation = SourceLocation(start),
      fields = objectField().map {
        GQLObjectField(
            sourceLocation = SourceLocation(start),
            name = it.name().text,
            value = it.value().parse())
      }
  )
}

private fun GraphQLParser.ListValueContext.parse(): GQLListValue {
  return GQLListValue(
      sourceLocation = SourceLocation(start),
      values = value().map { it.parse() }
  )
}

private fun GraphQLParser.VariableContext.parse(): GQLVariableValue {
  return GQLVariableValue(
      sourceLocation = SourceLocation(start),
      name = name().text
  )
}

private fun GraphQLParser.IntValueContext.parse() = GQLIntValue(
    sourceLocation = SourceLocation(start),
    value = text.toInt()
)

private fun GraphQLParser.FloatValueContext.parse() = GQLFloatValue(
    sourceLocation = SourceLocation(start),
    value = text.toDouble()
)

private fun GraphQLParser.BooleanValueContext.parse() = GQLBooleanValue(
    sourceLocation = SourceLocation(start),
    value = text.toBoolean()
)

private fun GraphQLParser.EnumValueContext.parse() = GQLEnumValue(
    sourceLocation = SourceLocation(start),
    value = name().text
)

private fun GraphQLParser.StringValueContext.parse(): GQLStringValue {
  return GQLStringValue(
      sourceLocation = SourceLocation(start),
      value = STRING()?.text?.removePrefix("\"")?.removeSuffix("\"")
          ?: BLOCK_STRING()?.text?.removePrefix("\"\"\"")?.removeSuffix("\"\"\"")
          ?: throw ParseException("Unrecognized string", start)
  )
}

private fun TerminalNode?.withoutQuotes() = this?.text?.removePrefix("\"")?.removeSuffix("\"")
private fun TerminalNode?.withoutBlockQuotes() = this?.text?.removePrefix("\"\"\"")?.removeSuffix("\"\"\"")?.trimIndent()

private fun GraphQLParser.TypeContext.parse(): GQLType {
  return namedType()?.parse()
      ?: nonNullType()?.parse()
      ?: listType()?.parse()
      ?: throw ParseException("Unrecognized type", start)
}

private fun GraphQLParser.ListTypeContext.parse(): GQLListType {
  return GQLListType(
      sourceLocation = SourceLocation(start),
      type = type().parse()
  )
}

private fun GraphQLParser.NamedTypeContext.parse(): GQLNamedType {
  return GQLNamedType(
      sourceLocation = SourceLocation(start),
      name = text
  )
}

private fun GraphQLParser.NonNullTypeContext.parse(): GQLNonNullType {
  return GQLNonNullType(
      sourceLocation = SourceLocation(start),
      type = namedType()?.parse()
          ?: listType()?.parse()
          ?: throw ParseException("Unrecognized on-null type", start)
  )
}

