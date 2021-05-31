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

private class State {
  val usedNames = mutableSetOf<String>()
  val collectedIrModelGroups = mutableListOf<IrModelGroup>()
}

private fun List<IrModelGroup>.flatten(): List<IrModelGroup> {
  val state = State()
  return map {
    it.walk2(state)
  } + state.collectedIrModelGroups
}

/**
 * walk2 potentially detaches the model groups
 */
private fun IrModelGroup.walk2(state: State): IrModelGroup {
  return copy(
      models = models.map { model ->
        val name = resolveNameClashes(state.usedNames, model.modelName)
        val nestedModelGroups = mutableListOf<IrModelGroup>()
        model.modelGroups.forEach { modelGroup ->
          if (modelGroup.models.singleOrNull()?.modelName == "Fragments") {
            nestedModelGroups.add(modelGroup)
          } else {
            state.collectedIrModelGroups.add(modelGroup.walk2(state))
          }
        }
        model.copy(
            modelName = name,
            modelGroups = nestedModelGroups
        )
      }
  )
}

/**
 * walk traverses the models until it reaches the desired depth
 */
private fun List<IrModelGroup>.walk(depth: Int, atDepth: Int): List<IrModelGroup> {
  if (depth == atDepth) {
    return flatten()
  } else {
    return map { modelGroup ->
      modelGroup.walk(depth, atDepth)
    }
  }
}

private fun IrModelGroup.walk(depth: Int, atDepth: Int): IrModelGroup {
  return copy(
      models = models.map { model ->
        model.copy(
            modelGroups = model.modelGroups.walk(depth + 1, atDepth)
        )
      }
  )
}

internal fun IrModelGroup.maybeFlatten(flatten: Boolean, atDepth: Int = 0): List<IrModelGroup> {
  return if (flatten) {
    listOf(this).walk(0, atDepth)
  } else {
    listOf(this)
  }
}