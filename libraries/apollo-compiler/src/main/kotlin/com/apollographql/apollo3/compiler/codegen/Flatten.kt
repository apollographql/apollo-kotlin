package com.apollographql.apollo3.compiler.codegen

import com.apollographql.apollo3.compiler.codegen.kotlin.experimental.ExplicitlyRemovedNode
import com.apollographql.apollo3.compiler.ir.IrModel
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

internal fun walk(
    initial: Boolean,
    node: ExplicitlyRemovedNode,
    startFromIrModelGroups: List<IrModelGroup>,
    matchedIrModelGroups: MutableList<ArrayDeque<IrModelGroup>>,
    matchedIrModels: MutableList<ArrayDeque<IrModel>>,
) {
  startFromIrModelGroups.forEach { startFromIrModelGroup ->
    if (initial) {
      // Initial case is strange where the first Class name will "match" the baseModelId
      if (!matchName(node.name, startFromIrModelGroup.baseModelId)) {
        return
      }
      performInitialWalk(node, startFromIrModelGroup, startFromIrModelGroups, matchedIrModelGroups, matchedIrModels)
    } else {
      startFromIrModelGroup.models.forEach { startFromIm ->
        // Check for the name match
        if (matchName(node.name, startFromIm.modelName)) {
          // Matched, add to the dequeues
          val irModelGroupDeque = matchedIrModelGroups.last()
          val irModelDeque = matchedIrModels.last()
          irModelGroupDeque.add(startFromIrModelGroup)
          irModelDeque.add(startFromIm)

          if (node.hasExtracted) {
            // Found a terminal node
            matchedIrModelGroups.add(ArrayDeque())
            matchedIrModels.add(ArrayDeque())

            node.children.forEach { (_, childNode) ->
              walk(false, childNode, startFromIm.modelGroups, matchedIrModelGroups, matchedIrModels)
            }

          } else {
            // Non-terminal, continue searching for more matches
            node.children.forEach { (_, childNode) ->
              walk(false, childNode, startFromIm.modelGroups, matchedIrModelGroups, matchedIrModels)
            }
          }
        }
      }
    }
  }
}

internal fun performInitialWalk(
    node: ExplicitlyRemovedNode,
    startFromIrModelGroup: IrModelGroup,
    startFromIrModelGroups: List<IrModelGroup>,
    matchedIrModelGroups: MutableList<ArrayDeque<IrModelGroup>>,
    matchedIrModels: MutableList<ArrayDeque<IrModel>>,
) {
  // Check if it is a fragment
  if (startFromIrModelGroup.baseModelId.startsWith("fragmentData.") || startFromIrModelGroup.baseModelId.startsWith("fragmentInterface.")) {
    performInitialFragmentWalk(node, startFromIrModelGroup, startFromIrModelGroups, matchedIrModelGroups, matchedIrModels)
  } else {
    node.children.forEach { (_, childNode) ->
      walk(false, childNode, startFromIrModelGroups, matchedIrModelGroups, matchedIrModels)
    }
  }
}

internal fun performInitialFragmentWalk(
    node: ExplicitlyRemovedNode,
    startFromIrModelGroup: IrModelGroup,
    startFromIrModelGroups: List<IrModelGroup>,
    matchedIrModelGroups: MutableList<ArrayDeque<IrModelGroup>>,
    matchedIrModels: MutableList<ArrayDeque<IrModel>>,
) {
  val followProcedure = sanityCheckFragment(node, startFromIrModelGroup)
  // We can't find anything to match, so just return
  if (followProcedure == -1) return

  if (followProcedure == 0) { // Nested Fragment
    walk(false, node, startFromIrModelGroups, matchedIrModelGroups, matchedIrModels)
  } else if (followProcedure == 1) {
    // Nested Data class, walk it like an operation
    node.children.forEach { (_, childNode) ->
      walk(false, childNode, startFromIrModelGroups, matchedIrModelGroups, matchedIrModels)
    }
    return
  }
}

internal fun sanityCheckFragment(node: ExplicitlyRemovedNode, startFromIrModelGroup: IrModelGroup): Int {
  return when {
    startFromIrModelGroup.models.size != 1 -> {
      -1
    }

    startFromIrModelGroup.models[0].modelName == node.name -> {
      0
    }

    startFromIrModelGroup.models[0].modelName == "Data" -> {
      1
    }

    else -> {
      -1
    }
  }
}

internal fun sanitizeAndRebuild(
    initialIrModelGroup: IrModelGroup,
    matchedIrModelGroups: MutableList<ArrayDeque<IrModelGroup>>,
    matchedIrModels: MutableList<ArrayDeque<IrModel>>,
): List<IrModelGroup> {
  if (matchedIrModelGroups.size < 2 || (matchedIrModelGroups.size != matchedIrModels.size)) {
    return listOf(initialIrModelGroup)
  }


  // Remove the empty dequeues
  val sanitizedIrModelGroups = matchedIrModelGroups.filter { it.isNotEmpty() }
  val sanitizedIrModels = matchedIrModels.filter { it.isNotEmpty() }

  // Ensure there is still parity
  if (sanitizedIrModelGroups.size < 1 || (sanitizedIrModelGroups.size != sanitizedIrModels.size)) {
    return listOf(initialIrModelGroup)
  }

  // Rebuild the model groups, extracting the top-line groups
  val rebuiltIrModelGroups = rebuildIrModelGroups(sanitizedIrModelGroups.toMutableList(), sanitizedIrModels.toMutableList())
  // Unfortunately, due to the way the rebuildIrModelGroups() function works, we need to do a final check for model name collisions
  return verifyRebuiltIrModelGroups(rebuiltIrModelGroups)
}

internal fun verifyRebuiltIrModelGroups(rebuiltIrModelGroups: List<IrModelGroup>): List<IrModelGroup> {
  val usedNames = mutableMapOf<Int, MutableSet<String>>() // Level -> Names
  return rebuiltIrModelGroups.map { irModelGroup ->
    verifyRebuiltIrModelGroups(irModelGroup, usedNames)
  }
}

private fun verifyRebuiltIrModelGroups(
    irModelGroup: IrModelGroup,
    usedNames: MutableMap<Int, MutableSet<String>>,
    level: Int = 0,
): IrModelGroup {
  return irModelGroup.copy(
      models = irModelGroup.models.map { irModel ->
        val name = resolveNameClashes(usedNames, irModel.modelName, level)
        irModel.copy(
            modelName = name,
            modelGroups = irModel.modelGroups.map { verifyRebuiltIrModelGroups(it, usedNames, level + 1) }
        )
      }
  )
}

internal fun rebuildIrModelGroups(
    matchedIrModelGroups: MutableList<ArrayDeque<IrModelGroup>>,
    matchedIrModels: MutableList<ArrayDeque<IrModel>>,
    stashed: MutableList<IrModelGroup> = mutableListOf(),
    queuedForExtraction: MutableList<IrModelGroup> = mutableListOf(),
    previousResults: List<IrModelGroup> = listOf(),
    rebuiltIrModelGroups: MutableList<IrModelGroup> = mutableListOf(),
): List<IrModelGroup> {
  // Store the baseline IrModelGroup dequeue
  val irModelGroups = matchedIrModelGroups.removeLastOrNull() ?: return rebuiltIrModelGroups // Reached the end
  val irModels = matchedIrModels.removeLastOrNull() ?: throw IllegalStateException("Imbalanced set of dequeues")

  // Data for later
  val initialSize = irModelGroups.size

  // Store the baseline IrModelGroup
  var extractedIrModelGroup =
      checkStashed(irModelGroups.removeLast(), irModelGroups, stashed)
  // Construct the last element in the irModels dequeues
  var terminalIrModel = getTerminalIrModel(previousResults, irModels, stashed, queuedForExtraction)

  // Check the queuedForExtraction lists for any matches
  terminalIrModel = checkQueuedForExtraction(terminalIrModel, queuedForExtraction)

  // After we finish updating terminalIrModel, make sure to update the IrModelGroup that contains it
  extractedIrModelGroup = extractedIrModelGroup.copy(
      models = extractedIrModelGroup.models.filterNot { it.modelName == terminalIrModel.modelName } + terminalIrModel
  )

  // Starting values from rebuilding this level
  var irModelGroup: IrModelGroup = extractedIrModelGroup
  var irModel: IrModel? = irModels.removeLastOrNull()

  // Remove the flattened IrModelGroup from the irModel
  irModel = irModel?.copy(
      modelGroups = irModel.modelGroups.filterNot { it.baseModelId == irModelGroup.baseModelId }
  )

  // Walk up the rest of the data doing the same replacement of values with the updated model groupings
  while (irModelGroups.isNotEmpty() && irModel != null) {
    irModelGroup = checkStashed(irModelGroups.removeLast(), irModelGroups, stashed)
    irModelGroup = irModelGroup.copy(
        models = irModelGroup.models.filterNot { it.modelName == irModel?.modelName } + irModel
    )
    irModel = irModels.removeLastOrNull()
    irModel = irModel?.copy(
        modelGroups = irModel.modelGroups.filterNot { it.baseModelId == irModelGroup.baseModelId } + irModelGroup
    )
  }

  // Add the extracted IrModelGroup to the list of flattened IrModelGroups
  rebuiltIrModelGroups.add(0, extractedIrModelGroup)

  // See if we are at the end of the dequeues
  if (matchedIrModelGroups.isEmpty()) {
    // If so, make sure we also include the head in our rebuilt list
    rebuiltIrModelGroups.add(0, irModelGroup)
  }

  val results = getResults(initialSize, irModelGroup, extractedIrModelGroup)

  return rebuildIrModelGroups(
      matchedIrModelGroups = matchedIrModelGroups,
      matchedIrModels = matchedIrModels,
      stashed = stashed,
      queuedForExtraction = queuedForExtraction,
      previousResults = results,
      rebuiltIrModelGroups = rebuiltIrModelGroups
  )
}

/**
 * If there is a stashed instance of the IrModelGroup which has been updated before, we should use this instead of the corresponding
 * IrModelGroup which is now containing stale information.
 */
internal fun checkStashed(
    removeLast: IrModelGroup,
    irModelGroups: ArrayDeque<IrModelGroup>,
    stashed: MutableList<IrModelGroup>,
): IrModelGroup {
  // Check if there is anything stashed to check & make sure this it the top-line IrModelGroup for this level of the rebuild
  if (stashed.isEmpty() || irModelGroups.isNotEmpty()) {
    return removeLast
  }

  // Check the stashed list for any matches
  val iter = stashed.iterator()
  while (iter.hasNext()) {
    val stashedIrModelGroup = iter.next()
    if (stashedIrModelGroup.baseModelId == removeLast.baseModelId) {
      // If we made a removal, we can stop looking for it in the future
      iter.remove()
      return stashedIrModelGroup
    }
  }
  // Nothing was found, safe to use the removeLast
  return removeLast
}

private fun getResults(
    initialSize: Int,
    irModelGroup: IrModelGroup,
    extractedIrModelGroup: IrModelGroup,
): List<IrModelGroup> {
  return if (initialSize == 1) {
    // Only want the possibly updated irModelGroup, since that is extractedIrModelGroup, but just with a potentially
    // updated terminalIrModel
    listOf(irModelGroup)
  } else {
    // Ordering is crucial. 0th index is the updated irModelGroup. 1st index is the model that was extracted
    listOf(irModelGroup, extractedIrModelGroup)
  }
}

private fun checkQueuedForExtraction(
    irModel: IrModel,
    queuedForExtraction: MutableList<IrModelGroup>,
): IrModel {
  var terminalIrModel = irModel
  val iter = queuedForExtraction.iterator()
  while (iter.hasNext()) {
    val queued = iter.next()
    val prevSize = terminalIrModel.modelGroups.size
    terminalIrModel = terminalIrModel.copy(
        modelGroups = terminalIrModel.modelGroups.filterNot { it.baseModelId == queued.baseModelId }
    )
    if (prevSize != terminalIrModel.modelGroups.size) {
      // If we made a removal, we can stop looking for it in the future
      iter.remove()
    }
  }
  return terminalIrModel
}

private fun getTerminalIrModel(
    previousResults: List<IrModelGroup>,
    irModels: ArrayDeque<IrModel>,
    stashed: MutableList<IrModelGroup>,
    queuedForExtraction: MutableList<IrModelGroup>,
): IrModel {
  // Pop of the last element in the irModels dequeues
  var terminalIrModel = irModels.removeLast()

  when (previousResults.size) {
    0 -> {
      // We are at the lowest level overall, so we can skip checks and just rebuild
    }

    1 -> {
      // Do a check to see if the terminalIrModel contains the same IrModelGroup as the previousResults[0], if so, we need to remove it
      val prevSize = terminalIrModel.modelGroups.size
      terminalIrModel = terminalIrModel.copy(
          modelGroups = terminalIrModel.modelGroups.filterNot { it.baseModelId == previousResults[0].baseModelId }
      )
      if (prevSize != terminalIrModel.modelGroups.size) {
        // If no match was made, we need ot stash this for later
        queuedForExtraction += previousResults[0]
      }
    }

    2 -> {
      // Do a check to see if the terminalIrModel contains the same IrModelGroup as the previousResults[0], if so, we need to remove it
      val prevSize = terminalIrModel.modelGroups.size
      val filtered = terminalIrModel.modelGroups.filterNot { it.baseModelId == previousResults[0].baseModelId }
      if (filtered.size == prevSize) {
        // If no match was made, we need ot stash this for a later update
        stashed += previousResults[0]
      } else {
        // If a match was made, we need to update the terminalIrModel
        terminalIrModel = terminalIrModel.copy(
            modelGroups = filtered + previousResults[0]
        )
      }
    }

    else -> {
      throw IllegalAccessError("rebuildIrModelGroups() should never result in a rebuild where size > 2")
    }
  }

  return terminalIrModel
}

private val operationSuffix = arrayOf("Query", "Subscription", "Mutation", "Impl")
private fun matchName(nodeName: String, modelName: String): Boolean {
  // Based on IrModelType naming
  val split = modelName.split('.')
  return if (split.size > 1) {
    // Since "Query", "Subscription" or "Mutation" are not always appended at this instance. operationBased can append "Impl" to the end
    split.any {
      nodeName == it || operationSuffix.any { suffix -> it == nodeName.removeSuffix(suffix) }
    }
  } else {
    nodeName == modelName
  }
}

private fun resolveNameClashes(usedNames: MutableMap<Int, MutableSet<String>>, modelName: String, level: Int): String {
  val names = usedNames.getOrPut(level) { mutableSetOf() }
  var i = 0
  var name = modelName
  while (names.contains(name)) {
    i++
    name = "$modelName$i"
  }
  names.add(name)
  return name
}

/**
 * Perform a check to see if the node structure matches the IrModelGroup and then allow for flattening by extracting the right pieces
 */
internal fun possiblyFlatten(node: ExplicitlyRemovedNode, initialIrModelGroup: IrModelGroup): List<IrModelGroup> {
  // Starting values
  val matchedImgs = mutableListOf(ArrayDeque<IrModelGroup>())
  val matchedIms = mutableListOf(ArrayDeque<IrModel>())

  // Initial node needs to be walked since it is not a valid starting point
  node.children.values.forEach { childNode ->
    walk(true, childNode, listOf(initialIrModelGroup), matchedImgs, matchedIms)
  }

  // Rebuild the nodes
  return sanitizeAndRebuild(initialIrModelGroup, matchedImgs, matchedIms)
}

internal fun IrModelGroup.maybeFlatten(
    flatten: Boolean,
    node: ExplicitlyRemovedNode?,
    atDepth: Int = 0,
    excludeNames: Set<String> = emptySet(),
): List<IrModelGroup> {
  return if (flatten) {
    listOf(this).walk(0, atDepth, excludeNames)
  } else if (node != null) {
    possiblyFlatten(node, this)
  } else {
    listOf(this)
  }
}
