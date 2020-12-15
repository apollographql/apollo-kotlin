package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.frontend.GQLField
import com.apollographql.apollo.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo.compiler.frontend.GQLSelectionSet
import com.apollographql.apollo.compiler.frontend.responseName

internal object FrontendIrMergeUtils {
  private fun GQLInlineFragment.merge(other: GQLInlineFragment): GQLInlineFragment {
    return this.copy(
        selectionSet = selectionSet.merge(other.selectionSet)
    )
  }

  private fun GQLSelectionSet.merge(other: GQLSelectionSet): GQLSelectionSet {
    val selectionsToAdd = other.selections.toMutableList()
    return copy(selections = selections.map { selection->
      when (selection) {
        is GQLFragmentSpread -> {
          // named fragments are easy to merge, just keep one of them
          selectionsToAdd.removeIf {
            (it as? GQLFragmentSpread)?.name == selection.name
          }
          selection
        }
        is GQLInlineFragment -> {
          val index = selectionsToAdd.indexOfFirst { (it as? GQLInlineFragment)?.typeCondition?.name == selection.typeCondition.name }
          if (index >= 0) {
            selection.merge(selectionsToAdd.removeAt(index) as GQLInlineFragment)

          } else {
            selection
          }
        }
        is GQLField -> {
          val index = selectionsToAdd.indexOfFirst { (it as? GQLField)?.responseName() == selection.responseName() }
          if (index >= 0) {
            selection.merge(selectionsToAdd.removeAt(index) as GQLField)

          } else {
            selection
          }
        }
      }
    } + selectionsToAdd)
  }

  private fun GQLField.merge(other: GQLField): GQLField {
    return this.copy(
        selectionSet = selectionSet?.merge(other.selectionSet!!)
    )
  }
}
