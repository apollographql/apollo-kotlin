package com.apollographql.apollo3.ast

class TraverseScope(
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
)

fun TraverseScope.addRequiredFields(operation: GQLOperationDefinition, parentType: String): GQLOperationDefinition {
  return operation.copy(
      selectionSet = addRequiredFields(operation.selectionSet, parentType)
  )
}

fun TraverseScope.addRequiredFields(fragmentDefinition: GQLFragmentDefinition, parentType: String): GQLFragmentDefinition {
  return fragmentDefinition.copy(
      selectionSet = addRequiredFields(fragmentDefinition.selectionSet, parentType)
  )
}

private fun TraverseScope.addRequiredFields(selectionSet: GQLSelectionSet, parentType: String, isTopLevel: Boolean = false): GQLSelectionSet {
  var newSelections = selectionSet.selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = addRequiredFields(it.selectionSet, it.typeCondition.name)
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> {
        val typeDefinition = it.definitionFromScope(schema, parentType)!!
        val newSelectionSet = it.selectionSet?.let {
          addRequiredFields(it, typeDefinition.type.leafType().name, true)
        }
        it.copy(
            selectionSet = newSelectionSet
        )
      }
    }
  }

  val hasFragment = selectionSet.selections.any { it is GQLFragmentSpread || it is GQLInlineFragment }

  val requiredFields = mutableSetOf<String>()
  if (hasFragment) {
    requiredFields.add("__typename")
  }

  /**
   * We should also
   */
  requiredFields += schema.keyFields(parentType)

  newSelections = if (hasFragment) {
    // remove the __typename if it exists
    // and add it again at the top so we're guaranteed to have it at the beginning of json parsing
    listOf(typeNameField) + newSelections.filterNot { (it as? GQLField)?.name == "__typename" }
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