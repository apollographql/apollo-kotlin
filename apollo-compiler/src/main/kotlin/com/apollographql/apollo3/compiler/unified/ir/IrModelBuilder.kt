package com.apollographql.apollo3.compiler.unified.ir

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.compiler.unified.codegen.CgLayout.Companion.modelName
import com.apollographql.apollo3.compiler.unified.codegen.CgLayout.Companion.upperCamelCaseIgnoringNonLetters

private data class FieldNode(
    val info: IrFieldInfo,
    val override: Boolean,
    val condition: BooleanExpression,
    val fieldSetNodes: List<FieldSetNode>,
    val modelId: IrModelId?,
)

private data class FieldSetNode(
    val id: IrModelId,
    val typeSet: TypeSet,
    val fields: List<FieldNode>,
    val possibleTypes: Set<String>,
    val accessors: List<IrAccessor>,
    val implements: List<IrModelId>,
    /**
     * Needed for ordering the type specs as well as naming the models
     */
    val isOther: Boolean,
    val isInterface: Boolean,
    val isFallback: Boolean,
)

private fun subpath(path: String, info: IrFieldInfo, typeSet: TypeSet, isOther: Boolean): String {
  val name = upperCamelCaseIgnoringNonLetters(typeSet.sorted() + info.responseName)

  return path + "." + (if (isOther) "Other" else "") + name
}

private class FieldNodeBuilder(
    val fragments: Map<String, IrField>,
) {
  private var cachedFragmentModelFields = mutableMapOf<String, FieldNode>()


  private fun getSuperFieldSetNodes(typeSet: TypeSet, candidates: Collection<FieldSetNode>): List<FieldSetNode> {
    val superTypeSets = superTypeSets(typeSet, candidates.map { it.typeSet })
    return candidates.filter {
      superTypeSets.contains(it.typeSet)
    }
  }

  fun buildOperation(
      field: IrField,
      operationName: String,
  ): FieldNode {
    val root = IrModelRoot(
        IrRootKind.Operation,
        operationName
    )
    return buildFieldNode(root, "", field, emptyList(), true)
  }

  private fun IrField.rename(newName: String): IrField {
    return copy(
        info = info.copy(alias = newName)
    )
  }

  fun buildFragmentInterface(name: String): FieldNode {
    return cachedFragmentModelFields.getOrPut(name) {
      val fragmentField = fragments[name] ?: error("Cannot find fragment $name")
      val root = IrModelRoot(
          IrRootKind.FragmentInterface,
          name
      )

      buildFieldNode(
          root,
          "",
          fragmentField.rename(name),
          emptyList(),
          false
      )
    }
  }

  fun buildFragmentImplementation(name: String): FieldNode {
    val ifaceField = buildFragmentInterface(name)
    val fragmentField = fragments[name] ?: error("Cannot find fragment $name")

    val root = IrModelRoot(
        IrRootKind.FragmentImplementation,
        name
    )

    return buildFieldNode(
        root,
        "",
        fragmentField,
        listOf(ifaceField),
        true
    )
  }

  class FieldState(
      val superFieldNodes: List<FieldNode>,
      val fragmentFieldNodes: List<FieldNode>,
      val root: IrModelRoot,
      val irField: IrField,
      val path: String,
  ) {
    val cachedFieldSetNodes = mutableMapOf<Entry, FieldSetNode>()
    val allTypeSets = irField.fieldSets.map { it.typeSet }

    fun fieldSetFor(typeSet: TypeSet): IrFieldSet {
      return irField.fieldSets.first { it.typeSet == typeSet }
    }
  }

  private fun getModelId(models: Collection<FieldSetNode>, typeSet: TypeSet): IrModelId {
    return models.filter { it.typeSet == typeSet }.let {
      val ret = it.firstOrNull { it.isInterface } ?: it.firstOrNull()
      check(ret != null) {
        "Cannot find base model"
      }
      ret
    }.id
  }

  private data class Entry(
      val isOther: Boolean,
      val isInterface: Boolean,
      val fieldSet: IrFieldSet,
  )

  /**
   * This builds the models greedily so that:
   * 1. we have a qualifiedName when needed
   * 2. we can build the "Other" fields
   */
  private fun buildFieldNode(
      root: IrModelRoot,
      path: String,
      field: IrField,
      superFieldNodes: List<FieldNode>,
      withImplementations: Boolean,
  ): FieldNode {
    if (field.fieldSets.isEmpty()) {
      return FieldNode(
          info = field.info,
          override = superFieldNodes.isNotEmpty(),
          condition = field.condition,
          fieldSetNodes = emptyList(),
          modelId = null
      )
    }

    val fragmentFieldNodes = mutableListOf<FieldNode>()
    val fragmentAccessors = mutableListOf<IrFragmentAccessor>()

    field.fragments.forEach {
      val fragmentFieldNode = buildFragmentInterface(it)
      fragmentFieldNodes.add(fragmentFieldNode)
      fragmentAccessors.add(
          IrFragmentAccessor(
              fragmentName = it,
              returnedModelId = fragmentFieldNode.modelId!!
          )
      )
    }

    val fieldState = FieldState(
        superFieldNodes,
        fragmentFieldNodes,
        root,
        field,
        path
    )

    val entries = mutableListOf<Entry>()
    if (withImplementations) {
      field.fieldSets.forEach {
        when {
          it.possibleTypes.isEmpty() && it.typeSet.size > 1 -> {
            entries.add(
                Entry(
                    fieldSet = it,
                    isInterface = true,
                    isOther = false,
                )
            )
          }
          subTypeCount(it.typeSet, fieldState.allTypeSets) == 0 -> {
            entries.add(
                Entry(
                    fieldSet = it,
                    isInterface = false,
                    isOther = false,
                )
            )
          }
          else -> {
            entries.add(
                Entry(
                    fieldSet = it,
                    isInterface = true,
                    isOther = false,
                )
            )
            entries.add(
                Entry(
                    fieldSet = it,
                    isInterface = false,
                    isOther = true,
                )
            )
          }
        }
      }
    } else {
      field.fieldSets.forEach {
        entries.add(
            Entry(
                fieldSet = it,
                isInterface = true,
                isOther = false,
            )
        )
      }
    }

    entries.forEach {
      buildFieldSetNode(
          fieldState,
          it,
      )
    }

    val fieldSetNodes = fieldState.cachedFieldSetNodes.values.sortedWith(Comparator { a, b ->
      /**
       * Sort by:
       * - Interfaces
       * - DataClasses
       * - Other
       */
      var r = a.isOther.compareTo(b.isOther)
      if (r != 0) {
        return@Comparator r
      }

      r = a.possibleTypes.isNotEmpty().compareTo(b.possibleTypes.isNotEmpty())
      if (r != 0) {
        return@Comparator r
      }

      return@Comparator a.typeSet.size - b.typeSet.size
    })

    val baseModelId = getModelId(fieldSetNodes, setOf(field.info.rawTypeName))
    val baseType = field.info.type.replacePlaceholder(baseModelId)

    val subtypeAccessors = fieldState.allTypeSets
        .filter {
          it.size > 1
        }.map {
          IrSubtypeAccessor(
              returnedModelId = getModelId(fieldSetNodes, it),
              typeSet = it - field.info.rawTypeName
          )
        }

    val accessors = fragmentAccessors + subtypeAccessors
    return FieldNode(
        info = field.info.copy(type = baseType),
        override = superFieldNodes.isNotEmpty(),
        condition = field.condition,
        fieldSetNodes = fieldSetNodes.map {
          if (it.id == baseModelId) {
            it.copy(accessors = accessors)
          } else {
            it
          }
        },
        modelId = baseModelId
    )
  }

  private fun IrType.replacePlaceholder(newId: IrModelId): IrType {
    return when (this) {
      is IrNonNullType -> IrNonNullType(ofType = ofType.replacePlaceholder(newId))
      is IrListType -> IrListType(ofType = ofType.replacePlaceholder(newId))
      is IrModelType -> copy(id = newId)
      else -> error("Not a compound type?")
    }
  }

  private fun buildFieldSetNode(
      state: FieldState,
      entry: Entry,
  ): FieldSetNode {
    val cached = state.cachedFieldSetNodes.get(entry)
    if (cached != null) {
      return cached
    }

    val typeSet = entry.fieldSet.typeSet
    val isOther = entry.isOther
    val isInterface = entry.isInterface
    val fieldSet = entry.fieldSet
    val superTypeSets = if (isOther) {
      setOf(typeSet)
    } else {
      strictlySuperTypeSets(fieldSet.typeSet, state.irField.fieldSets.map { it.typeSet })
    }

    val superSelfFieldSetNodes = superTypeSets.map { superTypeSet ->
      buildFieldSetNode(
          state,
          Entry(
              fieldSet = state.fieldSetFor(superTypeSet),
              isOther = false,
              isInterface = true
          ),
      )
    }

    val superFragmentFieldSetNodes = state.fragmentFieldNodes.flatMap { fieldNode ->
      getSuperFieldSetNodes(fieldSet.typeSet, fieldNode.fieldSetNodes)
    }
    val superSiblingFieldSetNodes = state.superFieldNodes.flatMap { fieldNode ->
      getSuperFieldSetNodes(fieldSet.typeSet, fieldNode.fieldSetNodes)
    }

    val implementedFieldSetNodes = (superSelfFieldSetNodes + superFragmentFieldSetNodes + superSiblingFieldSetNodes)

    val path = subpath(state.path, state.irField.info, fieldSet.typeSet, isOther)
    val fieldSetNode = FieldSetNode(
        id = IrModelId(state.root, path),
        accessors = emptyList(),
        fields = fieldSet.fields.map { childField ->
          buildFieldNode(
              root = state.root,
              path = path,
              field = childField,
              superFieldNodes = implementedFieldSetNodes.flatMap {
                it.fields.filter { it.info.responseName == childField.info.responseName }
              },
              withImplementations = !isInterface
          )
        },
        possibleTypes = fieldSet.possibleTypes,
        typeSet = fieldSet.typeSet,
        implements = implementedFieldSetNodes.map { it.id },
        isOther = isOther,
        isInterface = isInterface,
        isFallback = fieldSet.typeSet.size == 1 && isOther
    )

    state.cachedFieldSetNodes.put(entry, fieldSetNode)

    return fieldSetNode
  }
}

class IrModelGroupsBuilder(
    val fragments: Map<String, IrField>,
    val flatten: Boolean,
) {
  private val fieldNodeBuilder = FieldNodeBuilder(fragments)

  class Result(
      val modelGroups: List<IrModelGroup>,
      val rootModelId: IrModelId,
  )

  fun buildOperationModelGroups(
      field: IrField,
      operationName: String,
  ) = convertAndFlatten {
    fieldNodeBuilder.buildOperation(
        field,
        operationName
    )
  }

  fun buildFragmentInterfaceGroups(
      name: String,
  ) = convertAndFlatten {
    fieldNodeBuilder.buildFragmentInterface(
        name
    )
  }

  fun buildFragmentImplementationGroups(
      name: String,
  ) = convertAndFlatten {
    fieldNodeBuilder.buildFragmentImplementation(
        name
    )
  }

  private fun convertAndFlatten(block: () -> FieldNode): Result {
    val fieldNode = block()
    val modelGroups = fieldNode.toIrModelGroup() ?: error("Scalar root field")
    val maybeFlattened = if (flatten) {
      modelGroups.flatten(mutableSetOf())
    } else {
      listOf(modelGroups)
    }

    return Result(
        rootModelId = fieldNode.modelId!!,
        modelGroups = maybeFlattened
    )
  }
}

private fun FieldNode.toIrModelGroup(): IrModelGroup? {
  if (fieldSetNodes.isEmpty()) {
    return null
  }
  return IrModelGroup(
      baseModelId = modelId!!,
      models = fieldSetNodes.map { it.toIrModel(this) }
  )
}

private fun FieldSetNode.toIrModel(parentFieldNode: FieldNode): IrModel {
  return IrModel(
      modelName = modelName(parentFieldNode.info, typeSet, isOther),
      possibleTypes = possibleTypes,
      modelGroups = fields.mapNotNull { it.toIrModelGroup() },
      properties = fields.map { it.toIrProperty() },
      implements = implements,
      accessors = accessors,
      id = id,
      typeSet = typeSet,
      isInterface = isInterface,
      isBase = typeSet.size == 1 && !isOther,
      isFallback = isFallback,
  )
}

private fun FieldNode.toIrProperty(): IrProperty {
  var type = info.type
  if (condition != BooleanExpression.True) {
    // Consecutive IrNonNullType are most likely an error at this point but do not fail if
    // that happens
    while (type is IrNonNullType) {
      type = type.ofType
    }
  }
  return IrProperty(
      info = info.copy(type = type),
      override = override,
      condition = condition
  )
}

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
