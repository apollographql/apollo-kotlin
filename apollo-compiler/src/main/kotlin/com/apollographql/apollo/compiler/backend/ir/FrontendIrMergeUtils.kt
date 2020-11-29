package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.frontend.ir.Field
import com.apollographql.apollo.compiler.frontend.ir.InlineFragment

internal object FrontendIrMergeUtils {
  /**
   * Squashes fragments that defined on the same type condition into one:
   *
   * ```
   * query TestOperation {
   *   random {
   *       ... on Being {
   *           name
   *           friends {
   *               name
   *           }
   *       }
   *       ... on Wookie {
   *          race
   *          friends {
   *            lifeExpectancy
   *          }
   *       }
   *       ... on Being {
   *           friends {
   *              id
   *           }
   *       }
   *   }
   *}
   * ```
   *
   * fragments defined on `Being` are going to be squashed into 1 while fragment `on Wookie` remains intact
   */
  fun List<InlineFragment>.squashFragmentsWithSameTypeConditions(): List<InlineFragment> {
    return this
        .groupBy { fragment -> fragment.typeCondition }
        .map { (_, groupedFragments) ->
          groupedFragments.drop(1).fold(groupedFragments.first()) { result, fragment ->
            result.merge(fragment)
          }
        }
  }

  private fun List<InlineFragment>.mergeFragments(others: List<InlineFragment>): List<InlineFragment> {
    return this.plus(others)
        .groupBy { fragment -> fragment.typeCondition }
        .map { (_, groupedFragments) ->
          groupedFragments.drop(1).fold(groupedFragments.first()) { result, fragment ->
            result.merge(fragment)
          }
        }
  }

  private fun InlineFragment.merge(other: InlineFragment): InlineFragment {
    return this.copy(
        fields = this.fields.mergeFields(other.fields),
        inlineFragments = this.inlineFragments.mergeFragments(other.inlineFragments),
        fragments = this.fragments.plus(other.fragments).distinctBy { fragmentRef -> fragmentRef.name },
    )
  }

  private fun List<Field>.mergeFields(others: List<Field>): List<Field> {
    val fieldsToAdd = others.toMutableList()
    return this.map { field ->
      val fieldToMergeIndex = fieldsToAdd.indexOfFirst { otherField -> otherField.responseName == field.responseName }
      val fieldToMerge = if (fieldToMergeIndex >= 0) fieldsToAdd.removeAt(fieldToMergeIndex) else null
      if (fieldToMerge == null) {
        field
      } else {
        field.merge(fieldToMerge)
      }
    } + fieldsToAdd
  }

  private fun Field.merge(other: Field): Field {
    val mergedFields = this.fields.mergeFields(other.fields)
    val mergedInlineFragments = this.inlineFragments.mergeFragments(other.inlineFragments)
    val mergedFragments = this.fragmentRefs.plus(other.fragmentRefs).distinctBy { fragmentRef -> fragmentRef.name }
    return this.copy(
        fields = mergedFields,
        inlineFragments = mergedInlineFragments,
        fragmentRefs = mergedFragments,
    )
  }
}
