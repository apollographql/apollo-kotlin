package com.apollographql.apollo3.ast

internal fun GQLDocument.removeLocation(): GQLDocument = copy(
    definitions = definitions.map { it.removeLocation() },
    filePath = null
)

internal fun GQLDefinition.removeLocation(): GQLDefinition = when(this) {
  is GQLDirectiveDefinition -> this@removeLocation.removeLocation()
  is GQLEnumTypeExtension -> this@removeLocation.removeLocation()
  is GQLFragmentDefinition -> this@removeLocation.removeLocation()
  is GQLOperationDefinition -> this@removeLocation.removeLocation()
  is GQLInputObjectTypeExtension -> this@removeLocation.removeLocation()
  is GQLInterfaceTypeExtension -> this@removeLocation.removeLocation()
  is GQLObjectTypeExtension -> this@removeLocation.removeLocation()
  is GQLScalarTypeExtension -> this@removeLocation.removeLocation()
  is GQLSchemaDefinition -> this@removeLocation.removeLocation()
  is GQLSchemaExtension -> this@removeLocation.removeLocation()
  is GQLEnumTypeDefinition -> this@removeLocation.removeLocation()
  is GQLInputObjectTypeDefinition -> this@removeLocation.removeLocation()
  is GQLInterfaceTypeDefinition -> this@removeLocation.removeLocation()
  is GQLObjectTypeDefinition -> this@removeLocation.removeLocation()
  is GQLScalarTypeDefinition -> this@removeLocation.removeLocation()
  is GQLUnionTypeDefinition -> this@removeLocation.removeLocation()
  is GQLUnionTypeExtension -> this@removeLocation.removeLocation()
}

internal fun GQLEnumTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    enumValues = enumValues.map { it.removeLocation() }
)
internal fun GQLInputObjectTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    inputFields = inputFields.map { it.removeLocation() }
)
internal fun GQLInterfaceTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() }
)
internal fun GQLObjectTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields =  fields.map { it.removeLocation() }
)
internal fun GQLScalarTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
)
internal fun GQLUnionTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    memberTypes = memberTypes.map { it.removeLocation() }
)
internal fun GQLUnionTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    memberTypes = memberTypes.map { it.removeLocation() }
)

internal fun GQLFieldDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    arguments = arguments.map { it.removeLocation() },
    type = type.removeLocation()
)
internal fun GQLScalarTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
)

internal fun GQLSchemaDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    rootOperationTypeDefinitions = rootOperationTypeDefinitions.map { it.removeLocation() },
)

internal fun GQLOperationTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
)
internal fun GQLSchemaExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    operationTypesDefinition =operationTypesDefinition.map { it.removeLocation() },
)

internal fun GQLInterfaceTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() },
)

internal fun GQLInputObjectTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    inputFields = inputFields.map { it.removeLocation() },
)

internal fun GQLObjectTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() },
)

internal fun GQLOperationDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    selectionSet = selectionSet.removeLocation(),
    variableDefinitions = variableDefinitions.map { it.removeLocation() }
)

internal fun GQLVariableDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    type = type.removeLocation(),
    defaultValue = defaultValue?.removeLocation(),
    directives = directives.map { it.removeLocation() }
)

internal fun GQLFragmentDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    typeCondition = typeCondition.removeLocation(),
    selectionSet = selectionSet.removeLocation()
)

internal fun GQLSelectionSet.removeLocation(): GQLSelectionSet = copy(
    selections = selections.map { it.removeLocation() },
    sourceLocation = SourceLocation.UNKNOWN,
)

internal fun GQLArguments.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    arguments =  arguments.map { it.removeLocation() }
)

internal fun GQLSelection.removeLocation(): GQLSelection = when(this) {
  is GQLField -> copy(
      sourceLocation = SourceLocation.UNKNOWN,
      arguments = arguments?.removeLocation(),
      selectionSet = selectionSet?.removeLocation(),
      directives = directives.map { it.removeLocation() }
  )

  is GQLFragmentSpread -> copy(
      sourceLocation = SourceLocation.UNKNOWN,
      directives = directives.map { it.removeLocation() }
  )
  is GQLInlineFragment -> copy(
      sourceLocation = SourceLocation.UNKNOWN,
      typeCondition = typeCondition.removeLocation(),
      directives = directives.map { it.removeLocation() },
      selectionSet = selectionSet.removeLocation()
  )
}

internal fun GQLEnumTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    enumValues = enumValues.map { it.removeLocation() },
)

internal fun GQLEnumValueDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
)

internal fun GQLEnumValue.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
)

internal fun GQLDirective.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    arguments = arguments?.copy(arguments = arguments.arguments.map { it.removeLocation() })
)

internal fun GQLArgument.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    value = value.removeLocation()
)
internal fun GQLDirectiveDefinition.removeLocation(): GQLDirectiveDefinition = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    arguments = arguments.map { it.removeLocation() },
)


private fun GQLInputValueDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    type = type.removeLocation(),
    defaultValue = defaultValue?.removeLocation(),
)

internal fun GQLType.removeLocation() = when(this) {
  is GQLNonNullType -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLListType -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLNamedType -> removeLocation()
}

internal fun GQLNamedType.removeLocation() = copy(sourceLocation = SourceLocation.UNKNOWN)

internal fun GQLValue.removeLocation() = when (this) {
  is GQLBooleanValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLEnumValue -> removeLocation()
  is GQLFloatValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLIntValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLListValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLNullValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLObjectValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLStringValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLVariableValue -> copy(sourceLocation = SourceLocation.UNKNOWN)
}