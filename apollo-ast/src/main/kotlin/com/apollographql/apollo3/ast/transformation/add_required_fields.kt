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
import com.apollographql.apollo3.ast.isAbstract
import com.apollographql.apollo3.ast.rawType
import com.apollographql.apollo3.ast.rootTypeDefinition

fun addRequiredFields(
    operation: GQLOperationDefinition,
    addTypename: String,
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
): GQLOperationDefinition {
  val parentType = operation.rootTypeDefinition(schema)!!.name
  return operation.copy(
      selectionSet = operation.selectionSet.addRequiredFields(schema, addTypename, fragments, parentType, emptySet(), false)
  )
}

fun addRequiredFields(
    fragmentDefinition: GQLFragmentDefinition,
    addTypename: String,
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
): GQLFragmentDefinition {
  val newSelectionSet = fragmentDefinition.selectionSet.addRequiredFields(schema, addTypename, fragments, fragmentDefinition.typeCondition.name, emptySet(), true)

  return fragmentDefinition.copy(
      selectionSet = newSelectionSet,
  )
}

private fun GQLSelectionSet.isPolymorphic(schema: Schema, fragments: Map<String, GQLFragmentDefinition>, rootType: String): Boolean {
  return selections.any {
    when (it) {
      is GQLField -> false
      is GQLInlineFragment -> !schema.isTypeASuperTypeOf(it.typeCondition.name, rootType) || it.selectionSet.isPolymorphic(schema, fragments, rootType)
      is GQLFragmentSpread -> {
        val fragmentDefinition = fragments[it.name] ?: error("cannot find fragment ${it.name}")
        /**
         * If we were only looking at operationBased codegen, we wouldn't need to look inside fragment definitions but responseBased requires
         * the __typename at the root of the field to determine the shape
         */
        !schema.isTypeASuperTypeOf(fragmentDefinition.typeCondition.name, rootType) || fragmentDefinition.selectionSet.isPolymorphic(schema, fragments, rootType)
      }
    }
  }
}

/**
 * @param isRoot: whether this selection set is considered a valid root for adding __typename
 * This is the case for field selection sets but also fragments since fragments can be executed from the cache
 */
private fun GQLSelectionSet.addRequiredFields(
    schema: Schema,
    addTypename: String,
    fragments: Map<String, GQLFragmentDefinition>,
    parentType: String,
    parentFields: Set<String>,
    isRoot: Boolean,
): GQLSelectionSet {
  val selectionSet = this

  val requiresTypename = when(addTypename) {
    "ifPolymorphic" -> isRoot && isPolymorphic(schema, fragments, parentType)
    "ifFragments" -> selectionSet.selections.any { it is GQLFragmentSpread || it is GQLInlineFragment }
    "ifAbstract" -> isRoot && schema.typeDefinition(parentType).isAbstract()
    "always" -> isRoot
    else -> error("Unknown addTypename option: $addTypename")
  }
  val requiredFieldNames = schema.keyFields(parentType).toMutableSet()

  if (requiredFieldNames.isNotEmpty() || requiresTypename) {
    requiredFieldNames.add("__typename")
  }

  val fieldNames = parentFields + selectionSet.selections.filterIsInstance<GQLField>().map { it.name }.toSet()

  var newSelections = selectionSet.selections.map {
    when (it) {
      is GQLInlineFragment -> {
        it.copy(
            selectionSet = it.selectionSet.addRequiredFields(
                schema,
                addTypename,
                fragments,
                it.typeCondition.name,
                fieldNames + requiredFieldNames,
                false
            )
        )
      }
      is GQLFragmentSpread -> it
      is GQLField -> it.addRequiredFields(schema, addTypename, fragments, parentType)
    }
  }

  val fieldNamesToAdd = (requiredFieldNames - fieldNames).toMutableList()
  if (requiresTypename) {
    /**
     * For "ifFragments", we add the typename always, even if it's already in parentFields
     */
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

  newSelections = if (requiresTypename) {
    // remove the __typename if it exists
    // and add it again at the top, so we're guaranteed to have it at the beginning of json parsing
    // also remove any @include/@skip directive on __typename
    listOf(buildField("__typename")) + newSelections.filter { (it as? GQLField)?.name != "__typename" }
  } else {
    newSelections
  }

  return selectionSet.copy(
      selections = newSelections
  )
}

private fun GQLField.addRequiredFields(schema: Schema, addTypename: String, fragments: Map<String, GQLFragmentDefinition>, parentType: String): GQLField {
  val typeDefinition = definitionFromScope(schema, parentType)!!
  val newSelectionSet = selectionSet?.addRequiredFields(
      schema,
      addTypename,
      fragments,
      typeDefinition.type.rawType().name,
      emptySet(),
      true
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
