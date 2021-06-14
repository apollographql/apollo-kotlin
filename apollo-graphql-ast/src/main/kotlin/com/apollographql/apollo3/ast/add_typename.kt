package com.apollographql.apollo3.ast

class AddFieldsScope(
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
)

fun AddFieldsScope.addRequiredFields(operation: GQLOperationDefinition, parentType: String): GQLOperationDefinition {
  return operation.copy(
      selectionSet = addRequiredFields(operation.selectionSet, parentType, emptySet())
  )
}

fun AddFieldsScope.addRequiredFields(fragmentDefinition: GQLFragmentDefinition, parentType: String): GQLFragmentDefinition {
  return fragmentDefinition.copy(
      selectionSet = addRequiredFields(fragmentDefinition.selectionSet, parentType, emptySet())
  )
}

private fun AddFieldsScope.addRequiredFields(
    selectionSet: GQLSelectionSet,
    parentType: String,
    parentFields: Set<String>,
): GQLSelectionSet {
  val hasFragment = selectionSet.selections.any { it is GQLFragmentSpread || it is GQLInlineFragment }
  val requiredFields = schema.keyFields(parentType) + if (hasFragment) setOf("__typename") else emptySet()

  val newParentFields = parentFields + selectionSet.selections.filterIsInstance<GQLField>().map { it.name }.toSet()

  var newSelections = selectionSet.selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = addRequiredFields(it.selectionSet, it.typeCondition.name, newParentFields + requiredFields)
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

  val typesToAdd = requiredFields - newParentFields

  newSelections = newSelections + typesToAdd.map { buildField(it) }


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