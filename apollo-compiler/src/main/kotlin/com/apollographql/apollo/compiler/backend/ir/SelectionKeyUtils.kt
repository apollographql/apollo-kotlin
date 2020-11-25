package com.apollographql.apollo.compiler.backend.ir

internal object SelectionKeyUtils {

  fun List<BackendIr.Field>.addFieldSelectionKeys(selectionKeys: Set<SelectionKey>): List<BackendIr.Field> {
    return selectionKeys.fold(this) { fields, selectionKey ->
      fields.addFieldSelectionKey(selectionKey)
    }
  }

  fun List<BackendIr.Field>.addFieldSelectionKey(selectionKey: SelectionKey?): List<BackendIr.Field> {
    if (selectionKey == null) return this
    return this.map { field ->
      field.addFieldSelectionKey(selectionKey.plus(field.responseName))
    }
  }

  fun BackendIr.Field.addFieldSelectionKeys(selectionKeys: Set<SelectionKey>): BackendIr.Field {
    return selectionKeys.fold(this) { field, selectionKey ->
      field.addFieldSelectionKey(selectionKey)
    }
  }

  fun BackendIr.Field.addFieldSelectionKey(selectionKey: SelectionKey): BackendIr.Field {
    return this.copy(
        fields = this.fields.addFieldSelectionKey(selectionKey),
        fragments = this.fragments.addFragmentSelectionKey(selectionKey),
        selectionKeys = this.selectionKeys + selectionKey
    )
  }

  private fun List<BackendIr.Fragment>.addFragmentSelectionKey(selectionKey: SelectionKey): List<BackendIr.Fragment> {
    return this.map { fragment ->
      fragment.addFragmentSelectionKey(selectionKey.plus(fragment.name))
    }
  }

  private fun BackendIr.Fragment.addFragmentSelectionKey(selectionKey: SelectionKey): BackendIr.Fragment {
    return when (this) {
      is BackendIr.Fragment.Interface -> this.copy(
          fields = this.fields.addFieldSelectionKey(selectionKey),
          selectionKeys = this.selectionKeys + selectionKey
      )
      // it looks like a hack but for fragment implementations we don't allow to change their locations
      // (by adding a another selection key) as they must used only in one place where they defined.
      is BackendIr.Fragment.Implementation -> this
    }
  }
}
