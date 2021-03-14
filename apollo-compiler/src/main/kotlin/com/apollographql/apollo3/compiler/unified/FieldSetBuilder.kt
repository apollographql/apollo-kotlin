package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLSelectionSet
import com.apollographql.apollo3.compiler.frontend.Schema


class FieldSetsBuilder(
    val schema: Schema,
    val allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    val selectionSet: GQLSelectionSet,
    val typeCondition: String) {
  class Result(
      val fieldSets: List<IrFieldSet>,
      val usedReferences: UsedReferences,
  )

  private var usedNamedFragments = mutableSetOf<String>()
  private var usedEnums = mutableSetOf<String>()
  private var usedInputObjects = mutableSetOf<String>()
  private var usedCustomScalars = mutableSetOf<String>()

  fun build(): Result {
    TODO("Not yet implemented")
  }
}
