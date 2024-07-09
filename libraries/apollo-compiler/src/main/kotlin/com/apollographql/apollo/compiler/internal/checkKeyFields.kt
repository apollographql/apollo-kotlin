package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.rootTypeDefinition

private class CheckKeyFieldsScope(
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
) {
  private val implementedTypesCache = mutableMapOf<String, Set<String>>()
  fun implementedTypes(name: String) = implementedTypesCache.getOrPut(name) {
    schema.implementedTypes(name)
  }

  private val keyFieldsCache = mutableMapOf<String, Set<String>>()
  fun keyFields(name: String) = keyFieldsCache.getOrPut(name) {
    schema.keyFields(name)
  }
}

internal fun checkKeyFields(operation: GQLOperationDefinition, schema: Schema, allFragmentDefinitions: Map<String, GQLFragmentDefinition>) {
  val parentType = operation.rootTypeDefinition(schema)!!.name
  CheckKeyFieldsScope(schema, allFragmentDefinitions).checkField("Operation(${operation.name})", operation.selections, parentType)
}

internal fun checkKeyFields(
    fragmentDefinition: GQLFragmentDefinition,
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
) {
  CheckKeyFieldsScope(schema, allFragmentDefinitions).checkField("Fragment(${fragmentDefinition.name})", fragmentDefinition.selections, fragmentDefinition.typeCondition.name)
}

private fun CheckKeyFieldsScope.checkField(
    path: String,
    selections: List<GQLSelection>,
    parentType: String,
) {
  schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().forEach {
    checkFieldSet(path, selections, parentType, it.name)
  }
}

private fun CheckKeyFieldsScope.checkFieldSet(path: String, selections: List<GQLSelection>, parentType: String, possibleType: String) {
  val implementedTypes = implementedTypes(possibleType)

  val mergedFields = collectFields(selections, parentType, implementedTypes).groupBy {
    it.field.name
  }.values

  if (implementedTypes.contains(parentType)) {
    // only check types that are actually possible
    val fieldNames = mergedFields.map { it.first().field }
        .filter { it.alias == null }
        .map { it.name }.toSet()
    val keyFieldNames = keyFields(possibleType)

    val missingFieldNames = keyFieldNames.subtract(fieldNames)
    check(missingFieldNames.isEmpty()) {
      "Key Field(s) '$missingFieldNames' are not queried on $possibleType at $path"
    }
  }

  mergedFields.forEach {
    val first = it.first()
    val rawTypeName = first.field.definitionFromScope(schema, first.parentType)!!.type.rawType().name
    checkField(path + "." + first.field.name, it.flatMap { it.field.selections }, rawTypeName)
  }
}

private class FieldWithParent(val field: GQLField, val parentType: String)

private fun CheckKeyFieldsScope.collectFields(
    selections: List<GQLSelection>,
    parentType: String,
    implementedTypes: Set<String>,
): List<FieldWithParent> {
  if (selections.isEmpty()) {
    return emptyList()
  }
  if (!implementedTypes.contains(parentType)) {
    return emptyList()
  }
  return selections.flatMap {
    when (it) {
      is GQLField -> {
        if (it.directives.hasCondition()) {
          return@flatMap emptyList()
        }

        listOf(FieldWithParent(it, parentType))
      }
      is GQLInlineFragment -> {
        if (it.directives.hasCondition()) {
          return@flatMap emptyList()
        }

        collectFields(it.selections, it.typeCondition?.name ?: parentType, implementedTypes)
      }
      is GQLFragmentSpread -> {
        if (it.directives.hasCondition()) {
          return@flatMap emptyList()
        }

        val fragmentDefinition = allFragmentDefinitions[it.name]!!
        collectFields(fragmentDefinition.selections, fragmentDefinition.typeCondition.name, implementedTypes)
      }
    }
  }
}

private fun List<GQLDirective>?.hasCondition(): Boolean {
  return this?.any {
    it.name == "skip" && (it.arguments.first().value as? GQLStringValue)?.value != "false"
        || it.name == "include" && (it.arguments.first().value as? GQLStringValue)?.value != "true"
  } ?: false
}
