package com.apollographql.apollo3.ast

fun GQLOperationDefinition.withTypenameWhenNeeded(schema: Schema): GQLOperationDefinition {
  return copy(
      selectionSet = selectionSet.withTypenameWhenNeeded(schema)
  )
}

fun GQLFragmentDefinition.withTypenameWhenNeeded(schema: Schema): GQLFragmentDefinition {
  return copy(
      // Force the typename on all Fragments
      selectionSet = selectionSet.withTypenameWhenNeeded(schema, true)
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

private fun GQLSelectionSet.withTypenameWhenNeeded(schema: Schema, force: Boolean = false): GQLSelectionSet {
  var newSelections = selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = it.selectionSet.withTypenameWhenNeeded(schema)
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> it.copy(
          selectionSet = it.selectionSet?.withTypenameWhenNeeded(schema)
      )
    }
  }

  val hasFragment = selections.any { it is GQLFragmentSpread || it is GQLInlineFragment }

  newSelections = if (force || hasFragment) {
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
