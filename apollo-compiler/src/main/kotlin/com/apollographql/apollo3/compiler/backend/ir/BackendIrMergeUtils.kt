package com.apollographql.apollo3.compiler.backend.ir

internal object BackendIrMergeUtils {

  fun List<BackendIr.Field>.mergeFields(otherFields: List<BackendIr.Field>): List<BackendIr.Field> {
    val fieldsToAdd = otherFields.toMutableList()
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

  private fun BackendIr.Field.merge(otherField: BackendIr.Field): BackendIr.Field {
    val mergedFields = this.fields.mergeFields(otherField.fields)
    return this.copy(
        fields = mergedFields,
        fragments = this.fragments.copy(
            fragments = this.fragments.mergeFragments(
                parentFields = mergedFields,
                otherFragments = otherField.fragments
            ),
            accessors = this.fragments.accessors + otherField.fragments.accessors,
        ),
        selectionKeys = this.selectionKeys + otherField.selectionKeys,
    )
  }

  private fun List<BackendIr.Fragment>.mergeFragments(
      parentFields: List<BackendIr.Field>,
      otherFragments: List<BackendIr.Fragment>
  ): List<BackendIr.Fragment> {
    val fragmentsToAdd = otherFragments.toMutableList()
    return this.map { fragment ->
      val fragmentToMergeIndex = fragmentsToAdd.indexOfFirst { otherFragment ->
        otherFragment.name == fragment.name
      }
      val fragmentToMerge = if (fragmentToMergeIndex >= 0) fragmentsToAdd.removeAt(fragmentToMergeIndex) else null
      if (fragmentToMerge == null) {
        fragment
      } else {
        val fragmentFields = fragment.fields.mergeFields(fragmentToMerge.fields).mergeFields(parentFields)
        val nestedFragments = if (fragmentToMerge.nestedFragments == null) {
          fragment.nestedFragments
        } else {
          fragment.nestedFragments?.copy(
              fragments = fragment.nestedFragments.mergeFragments(
                  parentFields = fragmentFields,
                  otherFragments = fragmentToMerge.nestedFragments
              ),
              accessors = fragment.nestedFragments.accessors + fragmentToMerge.nestedFragments.accessors,
          ) ?: fragmentToMerge.nestedFragments
        }
        fragment.copy(
            fields = fragmentFields,
            nestedFragments = nestedFragments,
            selectionKeys = fragment.selectionKeys + fragmentToMerge.selectionKeys,
        )
      }
    } + fragmentsToAdd.map { fragment ->
      fragment.copy(
          fields = fragment.fields.mergeFields(parentFields),
      )
    }
  }
}
