package com.apollographql.apollo.compiler.backend.ir

internal object SelectionKeyUtils {

  fun List<BackendIr.Field>.addFieldSelectionKey(selectionKey: SelectionKey?): List<BackendIr.Field> {
    if (selectionKey == null) return this
    return this.map { field ->
      field.addFieldSelectionKey(selectionKey + field.responseName)
    }
  }

  fun BackendIr.Field.addFieldSelectionKey(selectionKey: SelectionKey?): BackendIr.Field {
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

  private fun List<BackendIr.Fragment>.addFragmentSelectionKey(selectionKey: SelectionKey): List<BackendIr.Fragment> {
    return this.map { fragment ->
      fragment.addFragmentSelectionKey(selectionKey)
    }
  }

  private fun BackendIr.Fragment.addFragmentSelectionKey(selectionKey: SelectionKey): BackendIr.Fragment {
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

  fun BackendIr.Field.isBelongToNamedFragment(namedFragmentName: String): Boolean {
    return selectionKeys.find { selectionKey ->
      selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root === namedFragmentName
    } != null
  }

  fun List<BackendIr.Field>.attachToNewSelectionRoot(selectionRootKey: SelectionKey): List<BackendIr.Field> {
    return this.map { field ->
      field.attachToNewSelectionRoot(selectionRootKey)
    }
  }

  private fun BackendIr.Field.attachToNewSelectionRoot(selectionRootKey: SelectionKey): BackendIr.Field {
    return this.copy(
        fields = this.fields.attachToNewSelectionRoot(selectionRootKey),
        fragments = this.fragments.copy(
            fragments = this.fragments.fragments.map { fragment -> fragment.attachToNewSelectionRoot(selectionRootKey) }
        ),
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { fieldKey -> fieldKey.type == selectionRootKey.type && fieldKey.root == selectionRootKey.root }
            .map { fieldKey -> selectionRootKey.merge(fieldKey) }
    )
  }

  private fun BackendIr.Fragment.attachToNewSelectionRoot(selectionRootKey: SelectionKey): BackendIr.Fragment {
    return this.copy(
        fields = this.fields.attachToNewSelectionRoot(selectionRootKey),
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { fieldKey -> fieldKey.type == selectionRootKey.type && fieldKey.root == selectionRootKey.root }
            .map { fieldKey -> selectionRootKey.merge(fieldKey) }
    )
  }

  /**
   * Merges 2 selection keys.
   * For instance:
   * ```
   *  [Query, Data, hero, Droid]
   *  [Query, Data, hero, friends, edges, node]
   *  ================================================
   *  [Query, Data, hero, Droid, friends, edges, node]
   *  ```
   */
  private fun SelectionKey.merge(otherKey: SelectionKey): SelectionKey {
    var index = 0
    do {
      index++
    } while (
        index < this.keys.size &&
        index < otherKey.keys.size &&
        this.keys[index].equals(otherKey.keys[index], ignoreCase = true)
    )
    return this.copy(
        keys = this.keys + otherKey.keys.subList(index, otherKey.keys.size)
    )
  }
}
