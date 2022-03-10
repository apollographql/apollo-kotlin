package com.apollographql.apollo3.api

class AdapterContext private constructor(
    private val variables: Executable.Variables?,
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

  class Builder {
    private var variables: Executable.Variables? = null

    fun variables(variables: Executable.Variables?) = apply {
      this.variables = variables
    }

    fun build(): AdapterContext {
      return AdapterContext(variables)
    }
  }
}
