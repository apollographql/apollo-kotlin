package com.apollographql.apollo.compiler.frontend.gql

fun GQLOperationDefinition.withTypenameWhenNeeded(schema: Schema): GQLOperationDefinition {
  return copy(
      selectionSet = selectionSet.withTypenameWhenNeeded(schema, true)
  )
}

fun GQLFragmentDefinition.withTypenameWhenNeeded(schema: Schema): GQLFragmentDefinition {
  return copy(
      // Fragment spread are not root selections by definition since they must be included by another selection set
      selectionSet = selectionSet.withTypenameWhenNeeded(schema, false)
  )
}

private val typeNameField = GQLField(
    name = "__typename",
    arguments = null,
    selectionSet = null,
    sourceLocation = SourceLocation.UNKNOWN,
    directives = emptyList(),
    alias = null
)

private fun GQLSelectionSet.withTypenameWhenNeeded(schema: Schema, isRootSelection: Boolean): GQLSelectionSet {

  var newSelections = selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = it.selectionSet.withTypenameWhenNeeded(schema, false)
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> it.copy(
          selectionSet = it.selectionSet?.withTypenameWhenNeeded(schema, true)
      )
    }
  }

  val hasFragment = selections.filter { it is GQLFragmentSpread || it is GQLInlineFragment }.isNotEmpty()

  newSelections = if (isRootSelection && hasFragment) {
    // remove the __typename if it exists
    // and add it again at the top so we're guaranteed to have it at the beginning of json parsing
    listOf(typeNameField) + newSelections.filterNot { (it as? GQLField)?.name == "__typename" }
  } else {
    newSelections
  }

  return copy(
      selections = newSelections
  )
}
