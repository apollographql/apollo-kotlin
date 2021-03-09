package com.apollographql.apollo3.compiler.backend.ir

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
                .addFragmentSelectionKey(selectionKey),
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
    return this.copy(
        fields = this.fields.addFieldSelectionKey(selectionKey + this.name),
        selectionKeys = this.selectionKeys + (selectionKey + this.name)
    )
  }

  fun List<BackendIr.Field>.removeFieldSelectionKeys(selectionKeysToRemove: Collection<SelectionKey>): List<BackendIr.Field> {
    if (selectionKeysToRemove.isEmpty()) return this
    return this.map { field ->
      field.removeFieldSelectionKeys(selectionKeysToRemove)
    }
  }

  fun BackendIr.Field.removeFieldSelectionKeys(selectionKeysToRemove: Collection<SelectionKey>): BackendIr.Field {
    if (selectionKeysToRemove.isEmpty()) return this
    return this.copy(
        fields = this.fields.removeFieldSelectionKeys(selectionKeysToRemove),
        fragments = this.fragments.copy(
            fragments = this.fragments.fragments
                .removeFragmentSelectionKeys(selectionKeysToRemove)
        ),
        selectionKeys = this.selectionKeys.filter { selectionKey ->
          selectionKeysToRemove.find { selectionKeyToRemove -> selectionKey.rootedWith(selectionKeyToRemove) } == null
        }.toSet()
    )
  }

  private fun List<BackendIr.Fragment>.removeFragmentSelectionKeys(selectionKeysToRemove: Collection<SelectionKey>): List<BackendIr.Fragment> {
    return this.map { fragment ->
      fragment.removeFragmentSelectionKeys(selectionKeysToRemove)
    }
  }

  fun BackendIr.Fragment.removeFragmentSelectionKeys(selectionKeysToRemove: Collection<SelectionKey>): BackendIr.Fragment {
    return this.copy(
        fields = this.fields.removeFieldSelectionKeys(selectionKeysToRemove),
        selectionKeys = this.selectionKeys.filter { selectionKey ->
          selectionKeysToRemove.find { selectionKeyToRemove -> selectionKey.rootedWith(selectionKeyToRemove) } == null
        }.toSet()
    )
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
    while (
        index < this.keys.size &&
        index < otherKey.keys.size &&
        this.keys[index].equals(otherKey.keys[index], ignoreCase = true)
    ) {
      index++
    }
    return this.copy(
        keys = this.keys + otherKey.keys.subList(index, otherKey.keys.size)
    )
  }

  private fun SelectionKey.rootedWith(root: SelectionKey): Boolean {
    if (this.type != root.type) return false

    var index = 0
    while (index < this.keys.size && index < root.keys.size) {
      if (!this.keys[index].equals(root.keys[index], ignoreCase = true)) {
        return false
      }
      index++
    }

    return index >= root.keys.size
  }
}
