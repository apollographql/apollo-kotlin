package com.apollographql.apollo3.ast.transformation

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelectionSet
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.ast.rootTypeDefinition

fun addRequiredFields(operation: GQLOperationDefinition, schema: Schema): GQLOperationDefinition {
  val parentType = operation.rootTypeDefinition(schema)!!.name
  return operation.copy(
      selectionSet = operation.selectionSet.addRequiredFields(schema, parentType, emptySet())
  )
}

fun addRequiredFields(fragmentDefinition: GQLFragmentDefinition, schema: Schema): GQLFragmentDefinition {
  return fragmentDefinition.copy(
      selectionSet = fragmentDefinition.selectionSet.addRequiredFields(schema, fragmentDefinition.typeCondition.name, emptySet())
  )
}

private fun GQLSelectionSet.addRequiredFields(schema: Schema, parentType: String, parentFields: Set<String>): GQLSelectionSet {
  val selectionSet = this
  val hasFragment = selectionSet.selections.any { it is GQLFragmentSpread || it is GQLInlineFragment }
  val requiredFieldNames = schema.keyFields(parentType).toMutableSet()

  if (requiredFieldNames.isNotEmpty()) {
    requiredFieldNames.add("__typename")
  }

  val fieldNames = parentFields + selectionSet.selections.filterIsInstance<GQLField>().map { it.name }.toSet()

  var newSelections = selectionSet.selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = it.selectionSet.addRequiredFields(
                schema,
                it.typeCondition.name,
                fieldNames + requiredFieldNames
            )
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> it.addRequiredFields(schema, parentType)
    }
  }

  val fieldNamesToAdd = (requiredFieldNames - fieldNames).toMutableList()
  if (hasFragment) {
    fieldNamesToAdd.add("__typename")
  }

  newSelections.filterIsInstance<GQLField>().forEach {
    /**
     * Verify that the fields we add won't overwrite an existing alias
     * This is not 100% correct as this validation should be made more globally
     */
    check(!fieldNamesToAdd.contains(it.alias)) {
      "Field ${it.alias}: ${it.name} in $parentType conflicts with key fields"
    }
  }
  newSelections = newSelections + fieldNamesToAdd.map { buildField(it) }

  newSelections = if (hasFragment) {
    // remove the __typename if it exists
    // and add it again at the top so we're guaranteed to have it at the beginning of json parsing
    val typeNameField = newSelections.firstOrNull { (it as? GQLField)?.name == "__typename" }
    listOfNotNull(typeNameField) + newSelections.filter { (it as? GQLField)?.name != "__typename" }
  } else {
    newSelections
  }

  return selectionSet.copy(
      selections = newSelections
  )
}

private fun GQLField.addRequiredFields(schema: Schema, parentType: String): GQLField {
  val typeDefinition = definitionFromScope(schema, parentType)!!
  val newSelectionSet = selectionSet?.addRequiredFields(
      schema,
      typeDefinition.type.leafType().name,
      emptySet()
  )

  return copy(
      selectionSet = newSelectionSet,
  )
}

private fun buildField(name: String): GQLField {
  return GQLField(
      name = name,
      arguments = null,
      selectionSet = null,
      sourceLocation = SourceLocation.UNKNOWN,
      directives = emptyList(),
      alias = null
  )
}