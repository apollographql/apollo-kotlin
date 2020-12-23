package com.apollographql.apollo.compiler.backend.ir

internal object SelectionKeyUtils {

  fun List<BackendIr.Field>.addFieldSelectionKey(selectionKey: SelectionKey?): List<BackendIr.Field> {
    if (selectionKey == null) return this
    return this.map { field ->
      field.addFieldSelectionKey(selectionKey + field.responseName)
    }
  }

  private fun BackendIr.Field.addFieldSelectionKey(selectionKey: SelectionKey?): BackendIr.Field {
    if (selectionKey == null) return this
    return this.copy(
        fields = this.fields.addFieldSelectionKey(selectionKey),
        fragments = this.fragments.copy(
            fragments = this.fragments.fragments
                .addFragmentSelectionKey(selectionKey)
                .map { fragment ->
                  fragment.copy(
                      selectionKeys = fragment.selectionKeys + selectionKey
                  )
                },
        ),
        selectionKeys = this.selectionKeys + selectionKey
    )
  }

  fun List<BackendIr.Fragment>.addFragmentSelectionKey(selectionKey: SelectionKey): List<BackendIr.Fragment> {
    return this.map { fragment ->
      fragment.addFragmentSelectionKey(selectionKey)
    }
  }

  fun BackendIr.Fragment.addFragmentSelectionKey(selectionKey: SelectionKey): BackendIr.Fragment {
    return when (this.type) {
      BackendIr.Fragment.Type.Interface -> this.copy(
          fields = this.fields.addFieldSelectionKey(selectionKey + this.name),
          selectionKeys = this.selectionKeys + (selectionKey + this.name)
      )
      // it looks like a hack but for fragment implementations we don't allow to change their locations
      // (by adding another selection key) as they must be used only in one place where they defined.
      else -> this
    }
  }
}
