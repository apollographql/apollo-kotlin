package com.apollographql.apollo.compiler.frontend.gql

/**
 * XXX: optimize to not visit the same fragment multiple times
 */
private class UsedFragmentNamesCollector(val schema: Schema, val fragmentDefinitions: Map<String, GQLFragmentDefinition>) {
  val typeDefinitions = schema.typeDefinitions

  fun operationUsedFragmentNames(operation: GQLOperationDefinition): Set<String> {
    return operation.usedFragmentNames()
  }

  private fun GQLOperationDefinition.usedFragmentNames(): Set<String> {
    return selectionSet.usedFragmentNames(rootTypeDefinition(schema)!!)

  }

  private fun GQLSelectionSet.usedFragmentNames(typeDefinitionInScope: GQLTypeDefinition): Set<String> {
    return selections.flatMap {
      when (it) {
        is GQLField -> it.usedFragmentNames(typeDefinitionInScope)
        is GQLInlineFragment -> it.usedFragmentNames()
        is GQLFragmentSpread -> it.usedFragmentNames()
      }
    }.toSet()
  }

  private fun GQLField.usedFragmentNames(typeDefinitionInScope: GQLTypeDefinition): Set<String> {
    val fieldTypeName = definitionFromScope(schema, typeDefinitionInScope)!!.type.leafType().name
    val fieldTypeDefinition = typeDefinitions[fieldTypeName]!!

    return (selectionSet?.usedFragmentNames(fieldTypeDefinition) ?: emptySet())
  }

  private fun GQLInlineFragment.usedFragmentNames() = selectionSet.usedFragmentNames(typeDefinitions[typeCondition.name]!!)

  private fun GQLFragmentSpread.usedFragmentNames(): Set<String> {
    return setOf(name) + fragmentDefinitions[name]!!.let{
      it.selectionSet.usedFragmentNames(typeDefinitions[it.typeCondition.name]!!)
    }
  }
}


/**
 * Return all named fragments
 */
fun GQLOperationDefinition.usedFragmentNames(schema: Schema, fragmentDefinitions: Map<String, GQLFragmentDefinition>) = UsedFragmentNamesCollector(schema, fragmentDefinitions).operationUsedFragmentNames(this)

