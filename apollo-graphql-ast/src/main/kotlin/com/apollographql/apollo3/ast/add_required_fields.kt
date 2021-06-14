package com.apollographql.apollo3.ast

private class AddFieldsScope(
    val schema: Schema,
)

fun addRequiredFields(operation: GQLOperationDefinition, schema: Schema): GQLOperationDefinition {
  val scope = AddFieldsScope(schema)
  val parentType = operation.rootTypeDefinition(schema)!!.name
  return operation.copy(
      selectionSet = scope.addRequiredFields(operation.selectionSet, parentType, emptySet())
  )
}

fun addRequiredFields(fragmentDefinition: GQLFragmentDefinition, schema: Schema): GQLFragmentDefinition {
  val scope = AddFieldsScope(schema)
  return fragmentDefinition.copy(
      selectionSet = scope.addRequiredFields(fragmentDefinition.selectionSet, fragmentDefinition.typeCondition.name, emptySet())
  )
}

private fun AddFieldsScope.addRequiredFields(
    selectionSet: GQLSelectionSet,
    parentType: String,
    parentFields: Set<String>,
): GQLSelectionSet {
  val hasFragment = selectionSet.selections.any { it is GQLFragmentSpread || it is GQLInlineFragment }
  val requiredFieldNames = schema.keyFields(parentType) + if (hasFragment) setOf("__typename") else emptySet()

  val fieldNames = parentFields + selectionSet.selections.filterIsInstance<GQLField>().map { it.name }.toSet()

  var newSelections = selectionSet.selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = addRequiredFields(it.selectionSet, it.typeCondition.name, fieldNames + requiredFieldNames)
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> {
        val typeDefinition = it.definitionFromScope(schema, parentType)!!
        val newSelectionSet = it.selectionSet?.let {
          addRequiredFields(it, typeDefinition.type.leafType().name, emptySet())
        }
        it.copy(
            selectionSet = newSelectionSet
        )
      }
    }
  }

  val fieldNamesToAdd = requiredFieldNames - fieldNames

  newSelections.filterIsInstance<GQLField>().forEach {
    /**
     * Verify that the fields we add won't overwrite an existing alias
     * This is not 100% correct as this validation should be made more globally
     */
    check(!fieldNamesToAdd.contains(it.alias)) {
      "Field ${it.alias}: ${it.name} in $parentType conflicts with key fields"
    }
  }
  newSelections = newSelections + fieldNamesToAdd.map { buildField(it) }

  newSelections = if (hasFragment) {
    // remove the __typename if it exists
    // and add it again at the top so we're guaranteed to have it at the beginning of json parsing
    newSelections.partition { (it as? GQLField)?.name == "__typename" }
        .let {
          listOf(it.first.first()) + it.second
        }
  } else {
    newSelections
  }

  return selectionSet.copy(
      selections = newSelections
  )
}


private fun buildField(name: String): GQLField {
  return GQLField(
      name = name,
      arguments = null,
      selectionSet = null,
      sourceLocation = SourceLocation.UNKNOWN,
      directives = emptyList(),
      alias = null
  )
}