package com.apollographql.apollo.ast.internal

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.*

/**
 * This implements listing 10 of https://tech.new-work.se/graphql-overlapping-fields-can-be-merged-fast-ea6e92e0a01
 */
internal fun IssuesScope.fieldsInSetCanMerge(
    operation: GQLOperationDefinition,
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
) {
  val context = ValidationContext(schema, fragments)

  val rootType = when (operation.operationType) {
    "query" -> schema.queryTypeDefinition
    "mutation" -> schema.mutationTypeDefinition
    "subscription" -> schema.subscriptionTypeDefinition
    else -> null
  }

  if (rootType == null) {
    return
  }

  val fieldMap = mutableMapOf<String, MutableSet<FieldAndType>>()
  collectFields(fieldMap, operation.selections, rootType, context)

  sameResponseShapeByName(fieldMap, emptyList(), context)
  sameForCommonParentsByName(fieldMap, emptyList(), context)
}

private fun IssuesScope.addFieldMergingIssue(path: List<String>, message: String, fields: Set<GQLField>) {
  fields.forEach {
    // TODO only register a single issue once we have issues that can support several source locations
    registerIssue(
        message = buildMessage(path, message),
        sourceLocation = it.sourceLocation,
    )
  }
}

private fun IssuesScope.addDifferentShapeIssue(path: List<String>, message: String, fields: Set<GQLField>) {
  fields.forEach {
    // TODO only register a single issue once we have issues that can support several source locations
    issues.add(
        DifferentShape(buildMessage(path, message), it.sourceLocation)
    )
  }
}

private data class ValidationContext(
    val schema: Schema,
    val fragments: Map<String, GQLFragmentDefinition>,
) {
  val visitedShapeSets = mutableSetOf<Set<FieldAndType>>()
  val visitedParentSets = mutableSetOf<Set<FieldAndType>>()
  val variableValuesInScope = mutableListOf<Map<String, GQLValue>>()
  val mergeSubSelectionsCache = mutableMapOf<Set<FieldAndType>, Map<String, MutableSet<FieldAndType>>>()

  fun pushFragment(arguments: List<GQLArgument>, variableDefinitions: List<GQLVariableDefinition>) {
    val newValues = mutableMapOf<String, GQLValue>()
    variableDefinitions.forEach { variableDefinition ->
      val argument = arguments.firstOrNull { it.name == variableDefinition.name }
      if (argument == null) {
        if (variableDefinition.defaultValue != null) {
          newValues.put(variableDefinition.name, variableDefinition.defaultValue)
        }
      } else {
        newValues.put(variableDefinition.name, substituteVariables(argument.value))
      }
    }
    variableValuesInScope.add(variableValuesInScope.lastOrNull().orEmpty() + newValues)
  }

  fun substituteVariables(value: GQLValue): GQLValue {
    return when (value) {
      is GQLBooleanValue -> value
      is GQLEnumValue -> value
      is GQLFloatValue -> value
      is GQLIntValue -> value
      is GQLListValue -> value.copy(values = value.values.map { substituteVariables(it) })
      is GQLNullValue -> value
      is GQLObjectValue -> value.copy(fields = value.fields.map { it.copy(value = substituteVariables(it.value)) })
      is GQLStringValue -> value
      is GQLVariableValue -> variableValue(value.name)
    }
  }

  fun popFragment() {
    variableValuesInScope.removeLast()
  }

  fun variableValue(variableName: String): GQLValue {
    // the variableName may not exist in the operation, which validation should catch
    return variableValuesInScope.lastOrNull()?.get(variableName) ?: GQLVariableValue(null, variableName)
  }
}

@OptIn(ApolloExperimental::class)
private fun collectFields(
    fieldMap: MutableMap<String, MutableSet<FieldAndType>>,
    selections: List<GQLSelection>,
    parentType: GQLTypeDefinition,
    context: ValidationContext,
) {
  for (selection in selections) {
    when (selection) {
      is GQLField -> {
        val responseName = selection.responseName()
        if (responseName !in fieldMap) {
          fieldMap[responseName] = mutableSetOf()
        }

        val fieldDefinition = selection.definitionFromScope(context.schema, parentType)
        val fieldType = fieldDefinition?.type

        fieldMap[responseName]!!.add(FieldAndType(selection, fieldType, parentType))
      }

      is GQLInlineFragment -> {
        val fragmentType = if (selection.typeCondition != null) {
          context.schema.typeDefinitions[selection.typeCondition.name]
        } else {
          parentType
        }
        // See ignores_unknown_types test
        if (fragmentType != null) {
          collectFields(fieldMap, selection.selections, fragmentType, context)
        }
      }

      is GQLFragmentSpread -> {
        val fragment = context.fragments[selection.name]
        if (fragment != null) {
          val fragmentType = context.schema.typeDefinitions[fragment.typeCondition.name]
          if (fragmentType != null) {
            if (selection.arguments.isNotEmpty() || fragment.variableDefinitions.isNotEmpty()) {
              context.pushFragment(selection.arguments, fragment.variableDefinitions)
              val selections = fragment.selections.map { it.substituteVariables(context) }
              collectFields(fieldMap, selections, fragmentType, context)
              context.popFragment()
            } else {
              // No fragment arguments, skip substituting the variables
              collectFields(fieldMap, fragment.selections, fragmentType, context)
            }
          }
        }
      }
    }
  }
}

private fun IssuesScope.sameResponseShapeByName(
    fieldMap: Map<String, Set<FieldAndType>>,
    currentPath: List<String>,
    context: ValidationContext,
) {
  for ((key, fieldAndTypes) in fieldMap) {
    val newPath = currentPath + key

    if (!requireSameOutputTypeShape(newPath, fieldAndTypes, context.schema)) {
      continue
    }

    if (!context.visitedShapeSets.add(fieldAndTypes)) {
      /**
       * We have visited this set before, do not recurse
       */
      continue
    }

    val subSelections = mergeSubSelections(fieldAndTypes, context)
    sameResponseShapeByName(subSelections, newPath, context)
  }
}

private fun mergeSubSelections(sameNameFields: Set<FieldAndType>, context: ValidationContext): Map<String, MutableSet<FieldAndType>> {
  val fieldMap = mutableMapOf<String, MutableSet<FieldAndType>>()
  for (fieldAndType in sameNameFields) {
    if (fieldAndType.field.selections.isNotEmpty() && fieldAndType.type != null) {
      val unwrappedType = fieldAndType.type.rawType()
      val typeDefinition = context.schema.typeDefinitions[unwrappedType.name]
      if (typeDefinition != null) {
        collectFields(fieldMap, fieldAndType.field.selections, typeDefinition, context)
      }
    }
  }
  return fieldMap
}

private fun IssuesScope.sameForCommonParentsByName(
    fieldMap: Map<String, Set<FieldAndType>>,
    currentPath: List<String>,
    context: ValidationContext,
) {
  for ((key, fieldAndTypes) in fieldMap) {
    val groups = groupByCommonParents(fieldAndTypes)
    val newPath = currentPath + key

    for (group in groups) {
      if (!requireSameNameAndArguments(newPath, group)) {
        continue
      }

      if (!context.visitedParentSets.add(group)) {
        /**
         * We have visited this set before, do not recurse
         */
        continue
      }

      val subSelections = mergeSubSelections(group, context)
      sameForCommonParentsByName(subSelections, newPath, context)
    }
  }
}

private fun groupByCommonParents(fields: Set<FieldAndType>): List<Set<FieldAndType>> {
  val abstractTypes = fields.filter {
    it.parentType is GQLInterfaceTypeDefinition || it.parentType is GQLUnionTypeDefinition
  }.toSet()

  val concreteTypes = fields.filter {
    it.parentType is GQLObjectTypeDefinition
  }.toSet()

  if (concreteTypes.isEmpty()) {
    return listOf(abstractTypes)
  }

  val groupsByConcreteParent = concreteTypes.groupBy { it.parentType }
  return groupsByConcreteParent.values.map { concreteGroup ->
    concreteGroup.toSet() + abstractTypes
  }
}

private fun IssuesScope.requireSameNameAndArguments(path: List<String>, fieldAndTypes: Set<FieldAndType>): Boolean {
  if (fieldAndTypes.size <= 1) {
    return true
  }

  var success = true
  var name: String? = null
  var arguments: List<GQLArgument>? = null
  val fields = fieldAndTypes.map { it.field }.toSet()

  for (fieldAndType in fieldAndTypes) {
    val field = fieldAndType.field

    if (name == null) {
      name = field.name
      arguments = field.arguments
      continue
    }

    if (field.name != name) {
      addFieldMergingIssue(path, "'$name' and '${field.name}' are different fields", fields)
      success = false
      continue
    }

    if (!sameArguments(field.arguments, arguments!!)) {
      addFieldMergingIssue(path, "they have different arguments", fields)
      success = false
    }
  }

  return success
}

private fun sameArguments(arguments1: List<GQLArgument>, arguments2: List<GQLArgument>): Boolean {
  if (arguments1.size != arguments2.size) {
    return false
  }

  for (argument in arguments1) {
    val matchedArgument = arguments2.firstOrNull { it.name == argument.name }
    if (matchedArgument == null) {
      return false
    }
    if (!areEqual(argument.value, matchedArgument.value)) {
      return false
    }
  }

  return true
}

private fun IssuesScope.requireSameOutputTypeShape(path: List<String>, fieldAndTypes: Set<FieldAndType>, schema: Schema): Boolean {
  if (fieldAndTypes.size <= 1) {
    return true
  }

  val fields = fieldAndTypes.map { it.field }.toSet()
  var typeAOriginal: GQLType? = null

  for (fieldAndType in fieldAndTypes) {
    if (typeAOriginal == null) {
      typeAOriginal = fieldAndType.type
      continue
    }

    var typeA: GQLType? = typeAOriginal
    var typeB: GQLType? = fieldAndType.type

    while (true) {
      val aIsNonNull = typeA is GQLNonNullType
      val bIsNonNull = typeB is GQLNonNullType
      if (aIsNonNull != bIsNonNull) {
        addDifferentShapeIssue(path, "they return different types: '${typeA?.toUtf8()}' and '${typeB?.toUtf8()}'", fields)
        return false
      }

      // Check list types
      val aIsList = typeA is GQLListType
      val bIsList = typeB is GQLListType
      if (aIsList != bIsList) {
        addDifferentShapeIssue(path, "they return different types: '${typeA?.toUtf8()}' and '${typeB?.toUtf8()}'", fields)
        return false
      }

      // Check if both are unwrapped (named types)
      val aIsNamed = typeA is GQLNamedType
      val bIsNamed = typeB is GQLNamedType
      if (aIsNamed && bIsNamed) {
        break
      }

      // Unwrap one layer
      typeA = when (typeA) {
        is GQLNonNullType -> typeA.type
        is GQLListType -> typeA.type
        else -> typeA
      }
      typeB = when (typeB) {
        is GQLNonNullType -> typeB.type
        is GQLListType -> typeB.type
        else -> typeB
      }
    }

    val nameA = typeA.name
    val nameB = typeB.name

    val aDefinition = schema.typeDefinitions.get(nameA)
    val bDefinition = schema.typeDefinitions.get(nameB)

    if (aDefinition?.isLeaf() == true || bDefinition?.isLeaf() == true) {
      if (nameA != nameB) {
        addDifferentShapeIssue(path, "they return different types: '${nameA}' and '${nameB}'", fields)
        return false
      }
    }
  }

  return true
}

private fun GQLTypeDefinition.isLeaf(): Boolean {
  return when (this) {
    is GQLScalarTypeDefinition, is GQLEnumTypeDefinition -> true
    else -> false
  }
}

private data class FieldAndType(
    val field: GQLField,
    val type: GQLType?,
    val parentType: GQLTypeDefinition,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false
    other as FieldAndType
    /**
     * A given field in the AST always has the same parentType so we can ignore it for equality checks
     */
    return field == other.field
  }

  override fun hashCode(): Int {
    return field.hashCode()
  }


  override fun toString(): String {
    return "${field.sourceLocation.pretty()}: ${field.toUtf8()} (${parentType.name})"
  }
}

private fun buildMessage(path: List<String>, message: String): String {
  return "Field at path `${path.joinToString(".")}` conflicts with another field: " +
      "$message. Use different aliases on the fields to fetch both if this was intentional."
}

@OptIn(ApolloExperimental::class)
private fun GQLSelection.substituteVariables(context: ValidationContext): GQLSelection {
  return when (this) {
    is GQLField -> copy(
        arguments = arguments.map { arg ->
          arg.copy(value = context.substituteVariables(arg.value))
        }
    )

    is GQLInlineFragment -> this
    is GQLFragmentSpread -> this
  }
}
