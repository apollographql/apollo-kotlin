package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLArgument
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.leafType
import com.apollographql.apollo3.compiler.capitalizeFirstLetter
import com.apollographql.apollo3.compiler.codegen.keyArgs
import com.apollographql.apollo3.compiler.codegen.paginationArgs

internal class SelectionSetsBuilder(
    val schema: Schema,
    val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    val toIr: GQLType.() -> IrType,
) {
  private val usedNames = mutableSetOf<String>()

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

  fun build(gqlSelections: List<GQLSelection>, parentType: String): List<IrSelectionSet> {
    return gqlSelections.walk("unused", true, parentType)
  }

  private class WalkResult(val self: IrSelection, val nested: List<IrSelectionSet>)

  /**
   * @param name the name to use for this [IrSelectionSet]. Must be unique for a given operation/fragment definition
   */
  private fun List<GQLSelection>.walk(name: String, isRoot: Boolean, parentType: String): List<IrSelectionSet> {
    val results = mapNotNull { it.walk(parentType) }

    return listOf(IrSelectionSet(name, isRoot, results.map { it.self })) + results.flatMap { it.nested }
  }

  private fun GQLSelection.walk(parentType: String): WalkResult? {
    return when (this) {
      is GQLField -> walk(parentType)
      is GQLInlineFragment -> walk()
      is GQLFragmentSpread -> walk()
    }
  }

  private fun GQLArgument.toIr(keyArgs: Set<String>, paginationArgs: Set<String>): IrArgument {
    return IrArgument(
        name = name,
        value = value.toIrValue(),
        isKey = keyArgs.contains(name),
        isPagination = paginationArgs.contains(name)
    )
  }

  private fun GQLField.walk(parentType: String): WalkResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val fieldDefinition = definitionFromScope(schema, parentType)!!

    val selectionSetName = resolveNameClashes(usedNames, name)
    return WalkResult(
        self = IrField(
            name = name,
            alias = alias,
            type = fieldDefinition.type.toIr(),
            arguments = arguments?.arguments.orEmpty().let { gqlArguments ->
              val typeDefinition = schema.typeDefinition(parentType)
              val keyArgs = typeDefinition.keyArgs(name, schema)
              val paginationArgs = typeDefinition.paginationArgs(name, schema)
              gqlArguments.map { gqlArgument ->
                gqlArgument.toIr(keyArgs, paginationArgs)
              }
            },
            condition = expression,
            selectionSetName = selectionSetName
        ),
        nested = selectionSet?.selections?.walk(selectionSetName, false, fieldDefinition.type.leafType().name).orEmpty()
    )
  }

  private fun GQLInlineFragment.walk(): WalkResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val name = "on${typeCondition.name.capitalizeFirstLetter()}"
    val selectionSetName = resolveNameClashes(usedNames, name)
    return WalkResult(
        self = IrFragment(
            typeCondition = typeCondition.name,
            // TODO: restrict the possible types to the possible types in the context of this selection
            possibleTypes = schema.possibleTypes(typeCondition.name),
            condition = expression,
            selectionSetName = selectionSetName,
            name = null
        ),
        nested = selectionSet.selections.walk(selectionSetName, false, typeCondition.name)
    )
  }

  private fun GQLFragmentSpread.walk(): WalkResult? {
    val expression = directives.toIncludeBooleanExpression()
    if (expression == BooleanExpression.False) {
      return null
    }

    val fragmentDefinition = allFragmentDefinitions[name]!!

    return WalkResult(
        self = IrFragment(
            typeCondition = fragmentDefinition.typeCondition.name,
            // TODO: restrict the possible types to the possible types in the context of this selection
            possibleTypes = schema.possibleTypes(fragmentDefinition.typeCondition.name),
            condition = expression,
            selectionSetName = null,
            name = name
        ),
        nested = emptyList()
    )
  }
}