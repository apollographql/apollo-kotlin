package com.apollographql.apollo.compiler.backend.ir

internal object FieldMergeUtils {

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
        args = (this.args + otherField.args).distinct(),
        fields = mergedFields,
        fragments = this.fragments.mergeInlineFragments(
            parentSelectionSet = mergedFields,
            otherFragments = otherField.fragments
        ),
        selectionKeys = this.selectionKeys + otherField.selectionKeys,
    )
  }

  private fun List<BackendIr.InlineFragment>.mergeInlineFragments(
      parentSelectionSet: List<BackendIr.Field>,
      otherFragments: List<BackendIr.InlineFragment>
  ): List<BackendIr.InlineFragment> {
    val fragmentsToAdd = otherFragments.toMutableList()
    return this.map { fragment ->
      val fragmentToMergeIndex = fragmentsToAdd.indexOfFirst { otherFragment ->
        otherFragment.name == fragment.name
      }
      val fragmentToMerge = if (fragmentToMergeIndex >= 0) fragmentsToAdd.removeAt(fragmentToMergeIndex) else null
      if (fragmentToMerge == null) {
        fragment
      } else {
        when (fragment) {
          is BackendIr.InlineFragment.Interface -> fragment.copy(
              fields = fragment.fields.mergeFields(fragmentToMerge.fields).mergeFields(parentSelectionSet),
          )
          is BackendIr.InlineFragment.Implementation -> fragment.copy(
              fields = fragment.fields.mergeFields(fragmentToMerge.fields).mergeFields(parentSelectionSet),
          )
        }
      }
    } + fragmentsToAdd.map { fragment ->
      when (fragment) {
        is BackendIr.InlineFragment.Interface -> fragment.copy(
            fields = fragment.fields.mergeFields(parentSelectionSet),
        )
        is BackendIr.InlineFragment.Implementation -> fragment.copy(
            fields = fragment.fields.mergeFields(parentSelectionSet),
        )
      }
    }
  }
}
