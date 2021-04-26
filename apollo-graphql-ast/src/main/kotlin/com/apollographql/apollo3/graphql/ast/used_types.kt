package com.apollographql.apollo3.graphql.ast

/**
 * This visits the fragments and operation recursively and all their
 * - fields
 * - variables
 *
 * For each field, it will return the leafType of the field and for each variable, the leaf type of the variable
 *
 * If one of the variable is an input object, it will visit all the input fields of the input object
 *
 * This returns all types, including the built-in ones, scalar, interface, etc... For codegen, we're mostly
 * interested in input/enum and these will be filtered later on
 *
 * There is no option to pass additional injected fragments as any types contributed by those should be handled
 * already
 */
fun List<GQLDefinition>.usedTypeNames(schema: Schema) = UsedTypeNamesScope(schema).usedTypeNames(this)

private class UsedTypeNamesScope(val schema: Schema) {
  val visitedInputTypes = mutableSetOf<String>()
  val visitedFragments = mutableSetOf<String>()
  val typeNames = mutableSetOf<String>()
  lateinit var fragmentDefinitions: Map<String, GQLFragmentDefinition>

  fun usedTypeNames(definitions: List<GQLDefinition>): Set<String> {
    val operations = definitions.filterIsInstance<GQLOperationDefinition>()
    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>()

    fragmentDefinitions = fragments.associateBy { it.name }

    operations.forEach {
      it.variableDefinitions.forEach {
        it.findUsedTypeNames()
      }
    }

    fragments.forEach { it.findUsedTypeNames() }
    operations.forEach { it.findUsedTypeNames() }

    return typeNames.toSet()
  }

  private fun GQLInputObjectTypeDefinition.findUsedTypeNames() {
    if (visitedInputTypes.contains(name)) {
      return
    }
    visitedInputTypes.add(name)

    typeNames.add(name)
    inputFields.forEach {
      val leafTypename = it.type.leafType().name
      when(val inputFieldTypeDefinition = schema.typeDefinition(leafTypename)) {
        is GQLInputObjectTypeDefinition -> inputFieldTypeDefinition.findUsedTypeNames()
        else -> typeNames.add(inputFieldTypeDefinition.name)
      }
    }
  }

  private fun GQLOperationDefinition.findUsedTypeNames() {
    return selectionSet.findUsedTypeNames(rootTypeDefinition(schema)!!)
  }

  private fun GQLVariableDefinition.findUsedTypeNames(){
    typeNames.add(type.leafType().name)
    (schema.typeDefinition(type.leafType().name) as? GQLInputObjectTypeDefinition)?.findUsedTypeNames()
  }

  private fun GQLSelectionSet.findUsedTypeNames(typeDefinitionInScope: GQLTypeDefinition) {
    return selections.forEach {
      when (it) {
        is GQLField -> it.findUsedTypeNames(typeDefinitionInScope)
        is GQLInlineFragment -> it.findUsedTypeNames()
        is GQLFragmentSpread -> it.findUsedTypeNames()
      }
    }
  }

  private fun GQLField.findUsedTypeNames(typeDefinitionInScope: GQLTypeDefinition) {
    val fieldTypeName = definitionFromScope(schema, typeDefinitionInScope)!!.type.leafType().name
    val fieldTypeDefinition = schema.typeDefinition(fieldTypeName)

    typeNames.add(fieldTypeName)
    selectionSet?.findUsedTypeNames(fieldTypeDefinition)
  }

  private fun GQLInlineFragment.findUsedTypeNames() = selectionSet.findUsedTypeNames(schema.typeDefinition(typeCondition.name))

  private fun GQLFragmentDefinition.findUsedTypeNames() {
    if (visitedFragments.contains(name)) {
      return
    }
    visitedFragments.add(name)

    selectionSet.findUsedTypeNames(schema.typeDefinition(typeCondition.name))
  }

  private fun GQLFragmentSpread.findUsedTypeNames() = fragmentDefinitions[name]?.findUsedTypeNames()
}

