package com.apollographql.apollo3.ast

fun GQLDocument.removeLocation(): GQLDocument = copy(
    definitions = definitions.map { it.removeLocation1() },
    filePath = null
)

fun GQLDefinition.removeLocation1(): GQLDefinition = when(this) {
  is GQLDirectiveDefinition -> removeLocation()
  is GQLEnumTypeExtension -> removeLocation()
  is GQLFragmentDefinition -> removeLocation()
  is GQLOperationDefinition -> removeLocation()
  is GQLInputObjectTypeExtension -> removeLocation()
  is GQLInterfaceTypeExtension -> removeLocation()
  is GQLObjectTypeExtension -> removeLocation()
  is GQLScalarTypeExtension -> removeLocation()
  is GQLSchemaDefinition -> removeLocation()
  is GQLSchemaExtension -> removeLocation()
  is GQLEnumTypeDefinition -> removeLocation()
  is GQLInputObjectTypeDefinition -> removeLocation()
  is GQLInterfaceTypeDefinition -> removeLocation()
  is GQLObjectTypeDefinition -> removeLocation()
  is GQLScalarTypeDefinition -> removeLocation()
  is GQLUnionTypeDefinition -> removeLocation()
  is GQLUnionTypeExtension -> removeLocation()
}

fun GQLEnumTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    enumValues = enumValues.map { it.removeLocation() }
)
fun GQLInputObjectTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    inputFields = inputFields.map { it.removeLocation() }
)
fun GQLInterfaceTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() }
)
fun GQLObjectTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields =  fields.map { it.removeLocation() }
)
fun GQLScalarTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
)
fun GQLUnionTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    memberTypes = memberTypes.map { it.removeLocation() }
)
fun GQLUnionTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    memberTypes = memberTypes.map { it.removeLocation() }
)

fun GQLFieldDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    arguments = arguments.map { it.removeLocation() },
    type = type.removeLocation()
)
fun GQLScalarTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
)

fun GQLSchemaDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    rootOperationTypeDefinitions = rootOperationTypeDefinitions.map { it.removeLocation() },
)

fun GQLOperationTypeDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
)
fun GQLSchemaExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    operationTypesDefinition =operationTypesDefinition.map { it.removeLocation() },
)

fun GQLInterfaceTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() },
)

fun GQLInputObjectTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    inputFields = inputFields.map { it.removeLocation() },
)

fun GQLObjectTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    fields = fields.map { it.removeLocation() },
)

fun GQLOperationDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    selectionSet = selectionSet.removeLocation(),
    variableDefinitions = variableDefinitions.map { it.removeLocation() }
)

fun GQLVariableDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    type = type.removeLocation(),
    defaultValue = defaultValue?.removeLocation(),
    directives = directives.map { it.removeLocation() }
)

fun GQLFragmentDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    typeCondition = typeCondition.removeLocation(),
    selectionSet = selectionSet.removeLocation()
)

fun GQLSelectionSet.removeLocation(): GQLSelectionSet = copy(
    selections = selections.map { it.removeLocation() },
    sourceLocation = SourceLocation.UNKNOWN,
)

fun GQLArguments.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    arguments =  arguments.map { it.removeLocation() }
)

fun GQLSelection.removeLocation(): GQLSelection = when(this) {
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

fun GQLEnumTypeExtension.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
    enumValues = enumValues.map { it.removeLocation() },
)

fun GQLEnumValueDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    directives = directives.map { it.removeLocation() },
)

fun GQLEnumValue.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
)

fun GQLDirective.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    arguments = arguments?.copy(arguments = arguments.arguments.map { it.removeLocation() })
)

fun GQLArgument.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    value = value.removeLocation()
)
fun GQLDirectiveDefinition.removeLocation(): GQLDirectiveDefinition = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    arguments = arguments.map { it.removeLocation() },
)


private fun GQLInputValueDefinition.removeLocation() = copy(
    sourceLocation = SourceLocation.UNKNOWN,
    type = type.removeLocation(),
    defaultValue = defaultValue?.removeLocation(),
)

fun GQLType.removeLocation() = when(this) {
  is GQLNonNullType -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLListType -> copy(sourceLocation = SourceLocation.UNKNOWN)
  is GQLNamedType -> removeLocation()
}

fun GQLNamedType.removeLocation() = copy(sourceLocation = SourceLocation.UNKNOWN)

fun GQLValue.removeLocation() = when (this) {
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