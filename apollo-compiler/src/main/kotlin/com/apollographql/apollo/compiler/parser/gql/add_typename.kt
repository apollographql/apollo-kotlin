package com.apollographql.apollo.compiler.parser.gql

fun GQLOperationDefinition.withTypenameWhenNeeded(schema: Schema): GQLOperationDefinition {
  val hasFragmentSpread = selectionSet.selections.filterIsInstance<GQLFragmentSpread>().isNotEmpty()

  return copy(
      selectionSet = selectionSet.withTypenameWhenNeeded(schema, hasFragmentSpread)
  )
}

fun GQLFragmentDefinition.withTypenameWhenNeeded(schema: Schema): GQLFragmentDefinition {
  return copy(
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

/**
 * XXX: add typename less often
 */
private fun GQLSelectionSet.withTypenameWhenNeeded(schema: Schema, needed: Boolean): GQLSelectionSet {

  var newSelections = selections.map {
    when (it) {
      is GQLInlineFragment -> {
        val neededInInlineFragments = it.selectionSet.selections.filterIsInstance<GQLInlineFragment>().isNotEmpty()
        it.copy(
            selectionSet = it.selectionSet.withTypenameWhenNeeded(schema, neededInInlineFragments)
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> it.copy(
          selectionSet = it.selectionSet?.withTypenameWhenNeeded(schema, true)
      )
    }
  }

  newSelections = if (needed) {
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
