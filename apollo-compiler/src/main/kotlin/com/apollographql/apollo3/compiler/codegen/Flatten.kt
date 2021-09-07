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

private fun List<IrModelGroup>.flatten(flattenNamesInOrder: Boolean, excludeNames: Set<String>): List<IrModelGroup> {
  val usedNames = excludeNames.toMutableSet()
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
private fun List<IrModelGroup>.walk(depth: Int, flattenNamesInOrder: Boolean, atDepth: Int, excludeNames: Set<String>): List<IrModelGroup> {
  return if (depth == atDepth) {
    flatten(flattenNamesInOrder, excludeNames)
  } else {
    map { modelGroup ->
      modelGroup.walk(depth, flattenNamesInOrder, atDepth, excludeNames)
    }
  }
}

private fun IrModelGroup.walk(depth: Int, flattenNamesInOrder: Boolean, atDepth: Int, excludeNames: Set<String>): IrModelGroup {
  return copy(
      models = models.map { model ->
        model.copy(
            modelGroups = model.modelGroups.walk(depth + 1, flattenNamesInOrder, atDepth, excludeNames)
        )
      }
  )
}

internal fun IrModelGroup.maybeFlatten(
    flatten: Boolean,
    flattenNamesInOrder: Boolean,
    atDepth: Int = 0,
    excludeNames: Set<String> = emptySet()
): List<IrModelGroup> {
  return if (flatten) {
    listOf(this).walk(0, flattenNamesInOrder, atDepth, excludeNames)
  } else {
    listOf(this)
  }
}