package com.apollographql.apollo3.ast

private class CheckKeyFieldsScope(
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
)

fun checkKeyFields(operation: GQLOperationDefinition, schema: Schema, allFragmentDefinitions: Map<String, GQLFragmentDefinition>) {
  val parentType = operation.rootTypeDefinition(schema)!!.name
  CheckKeyFieldsScope(schema, allFragmentDefinitions).checkField(operation.selectionSet.selections, parentType)
}

fun checkKeyFields(fragmentDefinition: GQLFragmentDefinition, schema: Schema, allFragmentDefinitions: Map<String, GQLFragmentDefinition>) {
  CheckKeyFieldsScope(schema, allFragmentDefinitions).checkField(fragmentDefinition.selectionSet.selections, fragmentDefinition.typeCondition.name)
}

private fun CheckKeyFieldsScope.checkField(
    selections: List<GQLSelection>,
    parentType: String,
) {
  schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>().forEach {
    checkFieldSet(selections, parentType, it.name )
  }
}

private fun CheckKeyFieldsScope.checkFieldSet(selections: List<GQLSelection>, parentType: String, possibleType: String) {
  val implementedTypes = schema.implementedTypes(possibleType)
  val mergedFields = collectFields(selections, parentType, implementedTypes).groupBy {
    it.field.name
  }.values

  val fieldNames = mergedFields.map { it.first().field }
      .filter { it.alias == null }
      .map { it.name }.toSet()
  val keyFieldNames = schema.keyFields(possibleType)

  val missingFieldNames = keyFieldNames.subtract(fieldNames)
  check (missingFieldNames.isEmpty()) {
    "Key Field(s) '$missingFieldNames' are not queried on $possibleType"
  }

  mergedFields.forEach {
    val first = it.first()
    val rawTypeName = first.field.definitionFromScope(schema, first.parentType)!!.type.leafType().name
    checkField(it.flatMap { it.field.selectionSet?.selections ?: emptyList() }, rawTypeName)
  }

}

private class FieldWithParent(val field: GQLField, val parentType: String)

private fun CheckKeyFieldsScope.collectFields(selections: List<GQLSelection>, parentType: String, implementedTypes: Set<String>): List<FieldWithParent> {
  return selections.flatMap {
    when(it) {
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

        if (implementedTypes.contains(it.typeCondition.name)) {
          collectFields(it.selectionSet.selections, it.typeCondition.name, implementedTypes)
        } else {
          emptyList()
        }
      }
      is GQLFragmentSpread -> {
        if (it.directives.hasCondition()) {
          return@flatMap emptyList()
        }

        val fragmentDefinition = allFragmentDefinitions[it.name]!!
        if (implementedTypes.contains(fragmentDefinition.typeCondition.name)) {
          collectFields(fragmentDefinition.selectionSet.selections, fragmentDefinition.typeCondition.name, implementedTypes)
        } else {
          emptyList()
        }
      }
    }
  }
}

private fun List<GQLDirective>?.hasCondition(): Boolean {
  return this?.any {
    it.name == "skip" && (it.arguments!!.arguments.first().value as GQLStringValue).value != "false"
        || it.name == "include" && (it.arguments!!.arguments.first().value as GQLStringValue).value != "true"
  } ?: false
}