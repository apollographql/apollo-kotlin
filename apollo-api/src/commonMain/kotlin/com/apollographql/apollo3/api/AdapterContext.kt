package com.apollographql.apollo3.api

class AdapterContext private constructor(
    private val variables: Executable.Variables?,
    private val mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?,
) {
  fun newBuilder() = Builder()
      .variables(variables)
      .mergedDeferredFragmentIds(mergedDeferredFragmentIds)

  fun variables(): Set<String> {
    if (variables == null) {
      return emptySet()
    }

    return variables.valueMap.filter {
      it.value == true
    }.keys
  }

  fun hasDeferredFragment(path: String, label: String?): Boolean {
    val sanitizedPath = path.trim('.')
    return mergedDeferredFragmentIds?.contains(DeferredFragmentIdentifier(sanitizedPath, label)) == true
  }

  class Builder {
    private var variables: Executable.Variables? = null
    private var mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>? = null

    fun variables(variables: Executable.Variables?) = apply {
      this.variables = variables
    }

    fun mergedDeferredFragmentIds(mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?) = apply {
      this.mergedDeferredFragmentIds = mergedDeferredFragmentIds
    }

    fun build(): AdapterContext {
      return AdapterContext(variables = variables, mergedDeferredFragmentIds = mergedDeferredFragmentIds)
    }
  }
}
