package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.compiler.ir.IrModelGroup

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

private fun List<IrModelGroup>.flatten(excludeNames: Set<String>): List<IrModelGroup> {
  val usedNames = excludeNames.toMutableSet()
  val collectedIrModelGroups = mutableListOf<IrModelGroup>()

  return map {
    it.walk2(usedNames, collectedIrModelGroups)
  } + collectedIrModelGroups
}

/**
 * walk2 potentially detaches the model groups
 */
private fun IrModelGroup.walk2(
    usedNames: MutableSet<String>,
    collectedIrModelGroups: MutableList<IrModelGroup>,
): IrModelGroup {
  return copy(
      models = models.map { model ->
        val name = resolveNameClashes(usedNames, model.modelName)

        val nestedModelGroups = mutableListOf<IrModelGroup>()
        model.modelGroups.forEach { modelGroup ->
          if (modelGroup.models.singleOrNull()?.modelName == "Fragments") {
            // Special case, "Fragments" are always nested
            nestedModelGroups.add(modelGroup)
          } else {
            /**
             * output the models in pre-order
             */
            val collected = mutableListOf<IrModelGroup>()
            collectedIrModelGroups.add(modelGroup.walk2(usedNames, collected))
            collectedIrModelGroups.addAll(collected)
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
private fun List<IrModelGroup>.walk(depth: Int, atDepth: Int, excludeNames: Set<String>): List<IrModelGroup> {
  return if (depth == atDepth) {
    flatten(excludeNames)
  } else {
    map { modelGroup ->
      modelGroup.walk(depth, atDepth, excludeNames)
    }
  }
}

private fun IrModelGroup.walk(depth: Int, atDepth: Int, excludeNames: Set<String>): IrModelGroup {
  return copy(
      models = models.map { model ->
        model.copy(
            modelGroups = model.modelGroups.walk(depth + 1, atDepth, excludeNames)
        )
      }
  )
}

internal fun IrModelGroup.maybeFlatten(
    flatten: Boolean,
    atDepth: Int = 0,
    excludeNames: Set<String> = emptySet()
): List<IrModelGroup> {
  return if (flatten) {
    listOf(this).walk(0, atDepth, excludeNames)
  } else {
    listOf(this)
  }
}