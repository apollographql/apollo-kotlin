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

  fun BackendIr.NamedFragment.SelectionSet.copyToDifferentSelectionRoot(
      currentRootSelectionKey: SelectionKey,
      newRootSelectionKey: SelectionKey,
  ): BackendIr.NamedFragment.SelectionSet {
    val patchedFields = this.fields.map { field ->
      field.copyToDifferentSelectionRoot(
          currentRootSelectionKey = currentRootSelectionKey,
          newRootSelectionKey = newRootSelectionKey,
      )
    }

    val patchedNestedFragments = this.fragments.copy(
        fragments = fragments.map { fragment ->
          fragment.copyToDifferentSelectionRoot(
              currentRootSelectionKey = currentRootSelectionKey,
              newRootSelectionKey = newRootSelectionKey,
          )
        }
    )

    val patchedSelectionKeys = this.selectionKeys + this.selectionKeys
        .filter { selectionKey -> selectionKey.startsWith(currentRootSelectionKey) }
        .map { key ->
          key.copy(
              root = newRootSelectionKey.root,
              keys = newRootSelectionKey.keys + key.keys.drop(currentRootSelectionKey.keys.size),
          )
        }

    return this.copy(
        fields = patchedFields,
        fragments = patchedNestedFragments,
        selectionKeys = patchedSelectionKeys,
    )
  }

  fun BackendIr.Field.copyToDifferentSelectionRoot(
      currentRootSelectionKey: SelectionKey,
      newRootSelectionKey: SelectionKey,
  ): BackendIr.Field {
    val patchedFields = this.fields.map { field ->
      field.copyToDifferentSelectionRoot(
          currentRootSelectionKey = currentRootSelectionKey,
          newRootSelectionKey = newRootSelectionKey,
      )
    }

    val patchedFragments = this.fragments.copy(
        fragments = this.fragments.copy(
            fragments = this.fragments.map { fragment ->
              fragment.copyToDifferentSelectionRoot(
                  currentRootSelectionKey = currentRootSelectionKey,
                  newRootSelectionKey = newRootSelectionKey,
              )
            },
        )
    )

    val patchedSelectionKeys = this.selectionKeys + this.selectionKeys
        .filter { selectionKey -> selectionKey.startsWith(currentRootSelectionKey) }
        .map { key ->
          key.copy(
              root = newRootSelectionKey.root,
              keys = newRootSelectionKey.keys + key.keys.drop(currentRootSelectionKey.keys.size),
          )
        }

    return this.copy(
        fields = patchedFields,
        fragments = patchedFragments,
        selectionKeys = patchedSelectionKeys,
    )
  }

  fun BackendIr.Fragment.copyToDifferentSelectionRoot(
      currentRootSelectionKey: SelectionKey,
      newRootSelectionKey: SelectionKey
  ): BackendIr.Fragment {
    val patchedFields = this.fields.map { field ->
      field.copyToDifferentSelectionRoot(
          currentRootSelectionKey = currentRootSelectionKey,
          newRootSelectionKey = newRootSelectionKey,
      )
    }

    val patchedNestedFragments = this.nestedFragments?.copy(
        fragments = this.nestedFragments.map { fragment ->
          fragment.copyToDifferentSelectionRoot(
              currentRootSelectionKey = currentRootSelectionKey,
              newRootSelectionKey = newRootSelectionKey,
          )
        },
    )

    val patchedSelectionKeys = this.selectionKeys + this.selectionKeys
        .filter { selectionKey -> selectionKey.startsWith(currentRootSelectionKey) }
        .map { key ->
          key.copy(
              root = newRootSelectionKey.root,
              keys = newRootSelectionKey.keys + key.keys.drop(currentRootSelectionKey.keys.size),
          )
        }

    return this.copy(
        fields = patchedFields,
        nestedFragments = patchedNestedFragments,
        selectionKeys = patchedSelectionKeys,
    )
  }

  private fun SelectionKey.startsWith(otherSelectionKey: SelectionKey): Boolean {
    if (this.type != otherSelectionKey.type) return false

    this.keys.forEachIndexed { index, key ->
      if (index >= otherSelectionKey.keys.size) {
        return true
      } else if (otherSelectionKey.keys[index] != key) {
        return false
      }
    }

    return true
  }
}
