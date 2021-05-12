package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.ir.IrModelGroup

private fun resolveNameClashes(usedNames: MutableSet<String>, modelName: String): String {
  var i = 0
  var name = modelName
  while (usedNames.contains(name)) {
    i++
    name = "$modelName$i"
  }
  usedNames.add(name)
  return name
}

private fun IrModelGroup.flatten(usedNames: MutableSet<String>): List<IrModelGroup> {
  return listOf(this.copy(
      models = models.map {
        it.copy(
            modelName = resolveNameClashes(usedNames, it.modelName),
            modelGroups = emptyList()
        )
      }
  )) + models.flatMap { it.modelGroups.flatMap { it.flatten(usedNames) } }
}

private fun IrModelGroup.flatten(): List<IrModelGroup> {
  return flatten(mutableSetOf())
}

internal fun IrModelGroup.maybeFlatten(flatten: Boolean): List<IrModelGroup> {
  return if (flatten) {
    flatten(mutableSetOf())
  } else {
    listOf(this)
  }
}