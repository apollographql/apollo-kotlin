package com.apollographql.apollo3.api

class AdapterContext private constructor(
    private val variables: Executable.Variables?,
    private val mergedDeferredFragmentIds: Set<DeferredFragmentIdentifier>?,
) {
  val currentPath = Path()

  fun newBuilder() = Builder().variables(variables)

  fun variables(): Set<String> {
    if (variables == null) {
      return emptySet()
    }

    return variables.valueMap.filter {
      it.value == true
    }.keys
  }

  fun hasDeferredFragment(path: Path, label: String?): Boolean {
    return mergedDeferredFragmentIds?.contains(DeferredFragmentIdentifier(path.toList(), label)) == true
  }

  class Builder {
    private var variables: Executable.Variables? = null
    private var deferredFragmentIdentifiers: Set<DeferredFragmentIdentifier>? = null

    fun variables(variables: Executable.Variables?) = apply {
      this.variables = variables
    }

    fun deferredFragmentLabels(deferredFragmentLabels: Set<DeferredFragmentIdentifier>) = apply {
      this.deferredFragmentIdentifiers = deferredFragmentLabels
    }

    fun build(): AdapterContext {
      return AdapterContext(variables = variables, mergedDeferredFragmentIds = deferredFragmentIdentifiers)
    }
  }

  class Path {
    /**
     * Values in the list can be either String or Int.
     */
    private val path = mutableListOf<Any>()

    fun push(objectPath: String) {
      path.add(objectPath)
    }

    fun push(arrayIndex: Int) {
      path.add(arrayIndex)
    }

    fun incrementArrayIndex() {
      val index = path.last() as Int
      path[path.lastIndex] = index + 1
    }

    fun pop() {
      path.removeAt(path.lastIndex)
    }

    fun toList(): List<Any> = path
  }
}
