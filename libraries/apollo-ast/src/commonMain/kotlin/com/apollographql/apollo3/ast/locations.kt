package com.apollographql.apollo.ast

internal fun GQLDocument.removeLocation(): GQLDocument = copy(
    definitions = definitions.map { it.removeLocation() },
    sourceLocation = null
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
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    enumValues = enumValues.map { it.removeLocation() }
)
internal fun GQLInputObjectTypeDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    inputFields = inputFields.map { it.removeLocation() }
)
internal fun GQLInterfaceTypeDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() }
)
internal fun GQLObjectTypeDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    fields =  fields.map { it.removeLocation() }
)
internal fun GQLScalarTypeDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
)
internal fun GQLUnionTypeDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    memberTypes = memberTypes.map { it.removeLocation() }
)
internal fun GQLUnionTypeExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    memberTypes = memberTypes.map { it.removeLocation() }
)

internal fun GQLFieldDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    arguments = arguments.map { it.removeLocation() },
    type = type.removeLocation()
)
internal fun GQLScalarTypeExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
)

internal fun GQLSchemaDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    rootOperationTypeDefinitions = rootOperationTypeDefinitions.map { it.removeLocation() },
)

internal fun GQLOperationTypeDefinition.removeLocation() = copy(
    sourceLocation = null,
)
internal fun GQLSchemaExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    operationTypesDefinition =operationTypeDefinitions.map { it.removeLocation() },
)

internal fun GQLInterfaceTypeExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() },
)

internal fun GQLInputObjectTypeExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    inputFields = inputFields.map { it.removeLocation() },
)

internal fun GQLObjectTypeExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() },
)

internal fun GQLOperationDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    selections = selections.removeSelectionsLocation(),
    variableDefinitions = variableDefinitions.map { it.removeLocation() }
)

internal fun GQLVariableDefinition.removeLocation() = copy(
    sourceLocation = null,
    type = type.removeLocation(),
    defaultValue = defaultValue?.removeLocation(),
    directives = directives.map { it.removeLocation() }
)

internal fun GQLFragmentDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    typeCondition = typeCondition.removeLocation(),
    selections = selections.removeSelectionsLocation()
)

private fun List<GQLSelection>.removeSelectionsLocation() = map { it.removeLocation() }

private fun List<GQLArgument>.removeArgumentsLocation() = map { it.removeLocation() }

internal fun GQLSelection.removeLocation(): GQLSelection = when(this) {
  is GQLField -> copy(
      sourceLocation = null,
      arguments = arguments.removeArgumentsLocation(),
      selections = selections.removeSelectionsLocation(),
      directives = directives.map { it.removeLocation() }
  )

  is GQLFragmentSpread -> copy(
      sourceLocation = null,
      directives = directives.map { it.removeLocation() }
  )
  is GQLInlineFragment -> copy(
      sourceLocation = null,
      typeCondition = typeCondition?.removeLocation(),
      directives = directives.map { it.removeLocation() },
      selections = selections.removeSelectionsLocation()
  )
}

internal fun GQLEnumTypeExtension.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
    enumValues = enumValues.map { it.removeLocation() },
)

internal fun GQLEnumValueDefinition.removeLocation() = copy(
    sourceLocation = null,
    directives = directives.map { it.removeLocation() },
)

internal fun GQLEnumValue.removeLocation() = copy(
    sourceLocation = null,
)

internal fun GQLDirective.removeLocation() = copy(
    sourceLocation = null,
    arguments = arguments.removeArgumentsLocation()
)

internal fun GQLArgument.removeLocation() = copy(
    sourceLocation = null,
    value = value.removeLocation()
)
internal fun GQLDirectiveDefinition.removeLocation(): GQLDirectiveDefinition = copy(
    sourceLocation = null,
    arguments = arguments.map { it.removeLocation() },
)


private fun GQLInputValueDefinition.removeLocation() = copy(
    sourceLocation = null,
    type = type.removeLocation(),
    defaultValue = defaultValue?.removeLocation(),
)

internal fun GQLType.removeLocation() = when(this) {
  is GQLNonNullType -> copy(sourceLocation = null)
  is GQLListType -> copy(sourceLocation = null)
  is GQLNamedType -> removeLocation()
}

internal fun GQLNamedType.removeLocation() = copy(sourceLocation = null)

internal fun GQLValue.removeLocation() = when (this) {
  is GQLBooleanValue -> copy(sourceLocation = null)
  is GQLEnumValue -> removeLocation()
  is GQLFloatValue -> copy(sourceLocation = null)
  is GQLIntValue -> copy(sourceLocation = null)
  is GQLListValue -> copy(sourceLocation = null)
  is GQLNullValue -> copy(sourceLocation = null)
  is GQLObjectValue -> copy(sourceLocation = null)
  is GQLStringValue -> copy(sourceLocation = null)
  is GQLVariableValue -> copy(sourceLocation = null)
}