package com.apollographql.apollo3.ast.transformation

import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLNode
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelectionSet
import com.apollographql.apollo3.ast.NodeTransformer
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.SourceLocation
import com.apollographql.apollo3.ast.TransformResult
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.ast.rootTypeDefinition
import com.apollographql.apollo3.ast.transform

/**
 * Add required fields:
 * - key fields for declarative cache
 * - __typename for polymorphic fields
 */
fun addRequiredFields(
    operation: GQLOperationDefinition,
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
): GQLOperationDefinition {
  val parentType = operation.rootTypeDefinition(schema)!!.name
  return operation.transform(
      AddRequiredFieldsTransformer(
          schema,
          allFragmentDefinitions,
          parentType,
          emptySet()
      )
  ) as GQLOperationDefinition
}

fun addRequiredFields(
    fragmentDefinition: GQLFragmentDefinition,
    schema: Schema,
    allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
): GQLFragmentDefinition {
  return fragmentDefinition.transform(
      AddRequiredFieldsTransformer(
          schema,
          allFragmentDefinitions,
          fragmentDefinition.typeCondition.name,
          emptySet()
      )
  ) as GQLFragmentDefinition
}

private class AddRequiredFieldsTransformer(
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    val parentType: String,
    val parentFields: Set<String>,
) : NodeTransformer {
  private fun requiresTypename(selectionSet: GQLSelectionSet): Boolean {
    val typeConditions = selectionSet.selections.filterIsInstance<GQLInlineFragment>().map {
      it.typeCondition.name
    }.toMutableList()
    typeConditions += selectionSet.selections.filterIsInstance<GQLFragmentSpread>().map {
      allFragmentDefinitions.get(it.name)!!.typeCondition.name
    }

    val implementedTypes = schema.implementedTypes(parentType)

    return typeConditions.any {
      !implementedTypes.contains(it)
    }
  }

  override fun transform(node: GQLNode): TransformResult {
    if (node !is GQLSelectionSet) {
      return TransformResult.Continue
    }
    val selectionSet = node
    val requiredFieldNames = schema.keyFields(parentType).toMutableSet()
    val requiresTypename = requiresTypename(selectionSet) || requiredFieldNames.isNotEmpty()

    val fieldNames = parentFields + selectionSet.selections.filterIsInstance<GQLField>().map { it.name }.toSet()

    var newSelections = selectionSet.selections.map {
      when (it) {
        is GQLInlineFragment -> {
          it.copy(
              selectionSet = it.selectionSet.transform(
                  AddRequiredFieldsTransformer(
                      schema,
                      allFragmentDefinitions,
                      it.typeCondition.name,
                      fieldNames + requiredFieldNames
                  )
              ) as GQLSelectionSet
          )
        }
        is GQLFragmentSpread -> it
        is GQLField -> {
          val typeDefinition = it.definitionFromScope(schema, parentType)!!
          val newSelectionSet = it.selectionSet?.transform(
              AddRequiredFieldsTransformer(
                  schema,
                  allFragmentDefinitions,
                  typeDefinition.type.leafType().name,
                  emptySet()
              )
          ) as GQLSelectionSet?
          it.copy(
              selectionSet = newSelectionSet
          )
        }
      }
    }

    val fieldNamesToAdd = (requiredFieldNames - fieldNames).toMutableSet()
    if (requiresTypename) {
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

    /**
     * This is done post-order because compat-based codegen requires `__typename` in individual inline fragments
     * This means that __typename is over-added in the query
     */
    newSelections = newSelections + fieldNamesToAdd.map { buildField(it) }

    newSelections = if (requiresTypename) {
      // remove the __typename if it exists
      // and add it again at the top so we're guaranteed to have it at the beginning of json parsing
      val typeNameField = newSelections.firstOrNull { (it as? GQLField)?.name == "__typename" }
      listOfNotNull(typeNameField) + newSelections.filter { (it as? GQLField)?.name != "__typename" }
    } else {
      newSelections
    }

    return TransformResult.Replace(
        selectionSet.copy(
            selections = newSelections
        )
    )
  }
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