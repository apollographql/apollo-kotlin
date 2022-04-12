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

  fun hasDeferredFragment(path: List<Any>, label: String?): Boolean {
    if (mergedDeferredFragmentIds == null) {
      // By default, parse all deferred fragments - this is the case when parsing from the normalized cache.
      return true
    }
    return mergedDeferredFragmentIds.contains(DeferredFragmentIdentifier(path, label))
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
