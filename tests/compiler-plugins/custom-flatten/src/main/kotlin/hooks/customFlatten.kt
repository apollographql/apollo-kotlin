package hooks

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

private fun List<IrModelGroup>.resolveNameClashes(): List<IrModelGroup> {
  val usedNames = mutableSetOf<String>()
  return map { modelGroup ->
    modelGroup.copy(
        models = modelGroup.models.map {
          it.copy(modelName = hooks.resolveNameClashes(usedNames, it.modelName))
        }
    )
  }
}

private fun List<IrModelGroup>.walk(state: State, depth: Int): List<IrModelGroup> {
  return map { modelGroup ->
    modelGroup.walk(state, depth)
  }
}

private fun IrModelGroup.walk(state: State, depth: Int): IrModelGroup {
  // -1 because we're still adding this depth only the next one is reset
  return if (depth >= state.maxDepth -1) {
    state.rootIrModelGroups.addAll(
        models.flatMap { it.modelGroups.walk(state, 0) }
    )
    copy(models = models.map {
      it.copy(
          modelGroups = emptyList()
      )
    })
  } else {
    copy(models = models.map {
      it.copy(
          modelGroups = it.modelGroups.walk(state, depth + 1))
    })
  }
}

private class State(
    val maxDepth: Int,
) {
  val rootIrModelGroups = mutableListOf<IrModelGroup>()
}

internal fun IrModelGroup.maybeFlatten(
    maxDepth: Int,
): IrModelGroup {
  return copy(models = models.map {
    val state = State(maxDepth)
    it.copy(modelGroups = (it.modelGroups.walk(state, 0) + state.rootIrModelGroups).resolveNameClashes())
  })
}