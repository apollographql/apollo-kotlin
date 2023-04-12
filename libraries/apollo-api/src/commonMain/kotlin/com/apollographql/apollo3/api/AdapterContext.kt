@file:JvmName("-AdapterContext")

package com.apollographql.apollo3.api

import kotlin.jvm.JvmName

class AdapterContext private constructor(
    private val variables: Set<String>?,
    private val mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?,
    val serializeVariablesWithDefaultBooleanValues: Boolean,
) {
  fun newBuilder() = Builder()
      .variables(variables)
      .mergedDeferredFragmentIds(mergedDeferredFragmentIds)
      .serializeVariablesWithDefaultBooleanValues(serializeVariablesWithDefaultBooleanValues)

  fun variables(): Set<String> {
    return variables.orEmpty()
  }

  fun hasDeferredFragment(path: List<Any>, label: String?): Boolean {
    if (mergedDeferredFragmentIds == null) {
      // By default, parse all deferred fragments - this is the case when parsing from the normalized cache.
      return true
    }
    return mergedDeferredFragmentIds.contains(DeferredFragmentIdentifier(path, label))
  }

  class Builder {
    private var variables: Set<String>? = null
    private var mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>? = null
    private var serializeVariablesWithDefaultBooleanValues: Boolean? = null

    fun variables(variables: Set<String>?) = apply {
      this.variables = variables
    }

    fun mergedDeferredFragmentIds(mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?) = apply {
      this.mergedDeferredFragmentIds = mergedDeferredFragmentIds
    }

    fun serializeVariablesWithDefaultBooleanValues(serializeVariablesWithDefaultBooleanValues: Boolean?) = apply {
      this.serializeVariablesWithDefaultBooleanValues = serializeVariablesWithDefaultBooleanValues
    }

    fun build(): AdapterContext {
      return AdapterContext(
          variables = variables,
          mergedDeferredFragmentIds = mergedDeferredFragmentIds,
          serializeVariablesWithDefaultBooleanValues = serializeVariablesWithDefaultBooleanValues == true
      )
    }
  }
}
