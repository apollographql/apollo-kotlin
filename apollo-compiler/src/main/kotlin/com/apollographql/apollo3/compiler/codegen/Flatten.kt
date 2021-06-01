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
}

private fun List<IrModelGroup>.flatten(flattenNamesInOrder: Boolean): List<IrModelGroup> {
  val usedNames = mutableSetOf<String>()
  val collectedIrModelGroups = mutableListOf<IrModelGroup>()

  return map {
    it.walk2(usedNames, collectedIrModelGroups, flattenNamesInOrder)
  } + collectedIrModelGroups
}

/**
 * walk2 potentially detaches the model groups
 *
 * @param flattenNamesInOrder Whether to resolve name clashes inorder (more intuitive) or not (2.x compatible)
 */
private fun IrModelGroup.walk2(
    usedNames: MutableSet<String>,
    collectedIrModelGroups: MutableList<IrModelGroup>,
    flattenNamesInOrder: Boolean,
): IrModelGroup {
  return copy(
      models = models.map { model ->
        val name = if (flattenNamesInOrder) resolveNameClashes(usedNames, model.modelName) else null

        val nestedModelGroups = mutableListOf<IrModelGroup>()
        model.modelGroups.forEach { modelGroup ->
          if (modelGroup.models.singleOrNull()?.modelName == "Fragments") {
            nestedModelGroups.add(modelGroup)
          } else {
            /**
             * output the models in pre-order
             */
            val collected = mutableListOf<IrModelGroup>()
            collectedIrModelGroups.add(modelGroup.walk2(usedNames, collected, flattenNamesInOrder))
            collectedIrModelGroups.addAll(collected)
          }
        }
        model.copy(
            /**
             * This tries to mimic the 2.x name resolution, which is slightly counter intuitive as the models encountered first
             * will have a larger index
             */
            modelName = name ?: resolveNameClashes(usedNames, model.modelName),
            modelGroups = nestedModelGroups
        )
      }
  )
}

/**
 * walk traverses the models until it reaches the desired depth
 */
private fun List<IrModelGroup>.walk(depth: Int, flattenNamesInOrder: Boolean, atDepth: Int): List<IrModelGroup> {
  if (depth == atDepth) {
    return flatten(flattenNamesInOrder)
  } else {
    return map { modelGroup ->
      modelGroup.walk(depth, flattenNamesInOrder, atDepth)
    }
  }
}

private fun IrModelGroup.walk(depth: Int, flattenNamesInOrder: Boolean, atDepth: Int): IrModelGroup {
  return copy(
      models = models.map { model ->
        model.copy(
            modelGroups = model.modelGroups.walk(depth + 1, flattenNamesInOrder, atDepth)
        )
      }
  )
}

internal fun IrModelGroup.maybeFlatten(flatten: Boolean, flattenNamesInOrder: Boolean, atDepth: Int = 0): List<IrModelGroup> {
  return if (flatten) {
    listOf(this).walk(0, flattenNamesInOrder, atDepth)
  } else {
    listOf(this)
  }
}