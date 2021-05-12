package com.apollographql.apollo3.compiler.ir

import com.apollographql.apollo3.api.BVariable
import com.apollographql.apollo3.api.BooleanExpression
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.ast.definitionFromScope
import com.apollographql.apollo3.ast.responseName

fun compileField(
    schema: Schema,
    allGQLFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    selections: List<GQLSelection>,
    rawTypeName: String,
): IrCompiledField {
  return CompileFieldScope(schema, allGQLFragmentDefinitions).build(selections, rawTypeName)
}

private class CompileFieldScope(
    private val schema: Schema,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
) {
  fun build(
      selections: List<GQLSelection>,
      rawTypeName: String,
  ): IrCompiledField {
    return buildField(
        name = "data",
        alias = null,
        arguments = emptyList(),
        type = IrNamedCompiledType(rawTypeName, true),
        selections = selections,
        condition = BooleanExpression.True,
    )
  }

  private fun buildField(
      name: String,
      alias: String?,
      arguments: List<IrCompiledArgument>,
      type: IrCompiledType,
      condition: BooleanExpression<BVariable>,
      selections: List<GQLSelection>,
  ): IrCompiledField {
    val fieldSets = if (selections.isNotEmpty()) {
      val rawTypeName = type.leafType().name
      val shapes = shapes(schema, allFragmentDefinitions, selections, rawTypeName)
      shapes.map { shape ->
        buildFieldSet(
            selections = selections,
            rawTypeName = rawTypeName,
            typeSet = shape.typeSet,
            possibleTypes = shape.possibleTypes,
        )
      }
    } else {
      emptyList()
    }

    return IrCompiledField(
        name = name,
        alias = alias,
        arguments = arguments,
        type = type,
        condition = condition,
        fieldSets = fieldSets,
    )
  }

  private fun buildFieldSet(
      selections: List<GQLSelection>,
      rawTypeName: String,
      typeSet: TypeSet,
      possibleTypes: PossibleTypes,
  ): IrCompiledFieldSet {
    val fields = collectFields(
        allFragmentDefinitions = allFragmentDefinitions,
        selections = selections,
        typenameInScope = rawTypeName,
        typeSet = typeSet,
    ).groupBy {
      it.gqlField.responseName()
    }.values.map { fieldsWithParentWithSameResponseName ->
      val first = fieldsWithParentWithSameResponseName.first().gqlField
      val fieldDefinition = first.definitionFromScope(schema, fieldsWithParentWithSameResponseName.first().parentType)!!

      val conditions = fieldsWithParentWithSameResponseName.map {
        it.gqlField.directives.toBooleanExpression()
      }
      val childSelections = fieldsWithParentWithSameResponseName.flatMap {
        it.gqlField.selectionSet?.selections ?: emptyList()
      }
      buildField(
          name = first.name,
          alias = first.alias,
          arguments = first.arguments?.arguments?.map { it.toIrCompiledArgument(schema, fieldDefinition) } ?: emptyList(),
          type = fieldDefinition.type.toCompiledIrType(schema),
          condition = BooleanExpression.Or(conditions.toSet()).simplify(),
          selections = childSelections,
      )
    }

    return IrCompiledFieldSet(
        typeSet = typeSet,
        possibleTypes = possibleTypes,
        compiledFields = fields,
    )
  }
}
