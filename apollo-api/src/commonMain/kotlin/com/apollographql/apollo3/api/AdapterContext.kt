package com.apollographql.apollo3.api

class AdapterContext private constructor(
    private val variables: Executable.Variables?,
    private val mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?,
) {
  fun newBuilder() = Builder().variables(variables)

  fun variables(): Set<String> {
    if (variables == null) {
      return emptySet()
    }

    return variables.valueMap.filter {
      it.value == true
    }.keys
  }

  fun hasDeferredFragment(path: String, label: String?): Boolean {
    return mergedDeferredFragmentIds?.contains(DeferredFragmentIdentifier(path, label)) == true
  }

  class Builder {
    private var variables: Executable.Variables? = null
    private var deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null

    fun variables(variables: Executable.Variables?) = apply {
      this.variables = variables
    }

    fun deferredFragmentIds(deferredFragmentIds: Set<DeferredFragmentIdentifier>) = apply {
      this.deferredFragmentIdentifiers = deferredFragmentIds
    }

    fun build(): AdapterContext {
      return AdapterContext(variables = variables, mergedDeferredFragmentIds = deferredFragmentIdentifiers)
    }
  }
}
