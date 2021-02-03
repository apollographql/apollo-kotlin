package com.apollographql.apollo.compiler.frontend.ir

import com.apollographql.apollo.compiler.frontend.GQLArgument
import com.apollographql.apollo.compiler.frontend.GQLBooleanValue
import com.apollographql.apollo.compiler.frontend.GQLDirective
import com.apollographql.apollo.compiler.frontend.GQLField
import com.apollographql.apollo.compiler.frontend.GQLFieldDefinition
import com.apollographql.apollo.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo.compiler.frontend.GQLFragmentSpread
import com.apollographql.apollo.compiler.frontend.GQLInlineFragment
import com.apollographql.apollo.compiler.frontend.GQLListType
import com.apollographql.apollo.compiler.frontend.GQLNamedType
import com.apollographql.apollo.compiler.frontend.GQLNonNullType
import com.apollographql.apollo.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo.compiler.frontend.GQLSelectionSet
import com.apollographql.apollo.compiler.frontend.GQLType
import com.apollographql.apollo.compiler.frontend.GQLTypeDefinition
import com.apollographql.apollo.compiler.frontend.GQLVariableDefinition
import com.apollographql.apollo.compiler.frontend.GQLVariableValue
import com.apollographql.apollo.compiler.frontend.Schema
import com.apollographql.apollo.compiler.frontend.SourceAwareException
import com.apollographql.apollo.compiler.frontend.definitionFromScope
import com.apollographql.apollo.compiler.frontend.findDeprecationReason
import com.apollographql.apollo.compiler.frontend.inferVariables
import com.apollographql.apollo.compiler.frontend.rootTypeDefinition
import com.apollographql.apollo.compiler.frontend.toUtf8
import com.apollographql.apollo.compiler.frontend.toUtf8WithIndents
import com.apollographql.apollo.compiler.frontend.usedFragmentNames
import com.apollographql.apollo.compiler.frontend.coerce

internal class FrontendIrBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    metadataFragmentDefinitions: List<GQLFragmentDefinition>,
    fragmentDefinitions: List<GQLFragmentDefinition>
) {
  private val allGQLFragmentDefinitions = (metadataFragmentDefinitions + fragmentDefinitions).associateBy {
    it.name
  }

  private val irFragmentDefinitions = fragmentDefinitions.map {
    it.toIr()
        .mergeFieldsAndInlineFragments()
        .simplifyConditions()
  }

  // For metadataFragments, we transform them to IR multiple times, in each module
  // There's no real alternative as we still need the GQLFragmentDefinition to perform validation
  private val allFragmentDefinitions = (irFragmentDefinitions + metadataFragmentDefinitions.map {
    it.toIr()
        .mergeFieldsAndInlineFragments()
        .simplifyConditions()
  }).associateBy { it.name }

  fun build(): FrontendIr {
    return FrontendIr(
        operations = operationDefinitions.map {
          it.toIr()
              .mergeFieldsAndInlineFragments()
              .simplifyConditions()
        },
        fragmentDefinitions = irFragmentDefinitions,
        allFragmentDefinitions = allFragmentDefinitions
    )
  }

  private fun GQLOperationDefinition.toIr(): FrontendIr.Operation {
    val typeDefinition = rootTypeDefinition(schema)
        ?: throw IllegalStateException("ApolloGraphql: cannot find root type for '$operationType'")

    val fragmentNames = usedFragmentNames(schema, allGQLFragmentDefinitions)

    return FrontendIr.Operation(
        name = name ?: throw IllegalStateException("Apollo doesn't support anonymous operation."),
        operationType = operationType.toIr(),
        variables = variableDefinitions.map { it.toIr() },
        typeDefinition = typeDefinition,
        selections = selectionSet.toIr(typeDefinition),
        description = description,
        sourceWithFragments = (toUtf8WithIndents() + "\n" + fragmentNames.joinToString(
            separator = "\n"
        ) { fragmentName ->
          allGQLFragmentDefinitions[fragmentName]!!.toUtf8WithIndents()
        }).trimEnd('\n'),
        gqlOperationDefinition = this
    )
  }

  private fun FrontendIr.Operation.mergeFieldsAndInlineFragments(): FrontendIr.Operation {
    return copy(
        selections = selections.mergeFieldsAndInlineFragments()
    )
  }

  private fun GQLFragmentDefinition.toIr(): FrontendIr.NamedFragmentDefinition {
    val typeDefinition = schema.typeDefinition(typeCondition.name)
    return FrontendIr.NamedFragmentDefinition(
        name = name,
        description = description,
        selections = selectionSet.toIr(typeDefinition),
        typeCondition = typeDefinition,
        source = toUtf8WithIndents(),
        gqlFragmentDefinition = this,
        variables = inferVariables(schema, allGQLFragmentDefinitions).map {
          FrontendIr.Variable(it.variable.name, null, it.expectedType.toIr())
        }
    )
  }

  private fun GQLVariableDefinition.toIr(): FrontendIr.Variable {
    return FrontendIr.Variable(
        name = name,
        defaultValue = defaultValue?.coerce(type, schema)?.orThrow(),
        type = type.toIr(),
    )
  }

  private fun GQLType.toIr(): FrontendIr.Type {
    return when (this) {
      is GQLNonNullType -> FrontendIr.Type.NonNull(ofType = type.toIr())
      is GQLListType -> FrontendIr.Type.List(ofType = type.toIr())
      is GQLNamedType -> FrontendIr.Type.Named(typeDefinition = schema.typeDefinition(name))
    }
  }

  private fun GQLSelectionSet.toIr(typeDefinition: GQLTypeDefinition): List<FrontendIr.Selection> {
    return selections.map {
      when (it) {
        is GQLFragmentSpread -> {
          it.toIr()
        }
        is GQLInlineFragment -> {
          it.toIr()
        }
        is GQLField -> {
          it.toIr(typeDefinition)
        }
      }
    }
  }

  private fun GQLField.toIr(typeDefinition: GQLTypeDefinition): FrontendIr.Selection.Field {
    val fieldDefinition = definitionFromScope(schema, typeDefinition)
        ?: throw IllegalStateException("ApolloGraphQL: cannot lookup field $name")
    val irType = fieldDefinition.type.toIr()

    return FrontendIr.Selection.Field(
        alias = alias,
        name = name,
        type = irType,
        selections = selectionSet?.toIr(irType.leafTypeDefinition) ?: emptyList(),
        condition = directives.toCondition(),
        description = fieldDefinition.description,
        deprecationReason = fieldDefinition.directives.findDeprecationReason(),
        arguments = arguments?.arguments?.map { it.toIr(fieldDefinition) } ?: emptyList()
    )
  }

  private fun GQLArgument.toIr(fieldDefinition: GQLFieldDefinition): FrontendIr.Argument {
    val inputValueDefinition = fieldDefinition.arguments.first { it.name == name }

    return FrontendIr.Argument(
        name = name,
        value = value.coerce(inputValueDefinition.type, schema).orThrow(),
        defaultValue = inputValueDefinition.defaultValue?.coerce(inputValueDefinition.type, schema)?.orThrow(),
        type = inputValueDefinition.type.toIr(),
        )
  }

  private fun List<FrontendIr.Selection>.mergeFieldsAndInlineFragments(): List<FrontendIr.Selection> {
    val irSelections = mutableListOf<FrontendIr.Selection>()

    forEach { selection ->
      when (selection) {
        is FrontendIr.Selection.InlineFragment -> {
          val index = irSelections.indexOfFirst { it is FrontendIr.Selection.InlineFragment && it.fragmentDefinition.typeCondition.name == selection.fragmentDefinition.typeCondition.name }
          if (index == -1) {
            irSelections.add(selection.copy(
                fragmentDefinition = selection.fragmentDefinition.mergeFieldsAndInlineFragments()
            ))
          } else {
            val irInlineFragment = irSelections.removeAt(index) as FrontendIr.Selection.InlineFragment
            irSelections.add(index, irInlineFragment.mergeWith(selection))
          }
        }
        is FrontendIr.Selection.FragmentSpread -> {
          val index = irSelections.indexOfFirst { it is FrontendIr.Selection.FragmentSpread && it.name == selection.name }
          if (index == -1) {
            irSelections.add(selection)
          } else {
            // It can happen that the same named fragment is requested multiple times
            // It's mostly useless but we still need to merge the conditions
            val irFragmentSpread = irSelections.removeAt(index) as FrontendIr.Selection.FragmentSpread
            irSelections.add(index, irFragmentSpread.mergeWith(selection))
          }
        }
        is FrontendIr.Selection.Field -> {
          val index = irSelections.indexOfFirst { it is FrontendIr.Selection.Field && it.responseName == selection.responseName }
          if (index == -1) {
            irSelections.add(selection.copy(
                selections = selection.selections.mergeFieldsAndInlineFragments()
            ))
          } else {
            val irField = irSelections.removeAt(index) as FrontendIr.Selection.Field
            irSelections.add(index, irField.mergeWith(selection))
          }
        }
      }
    }

    return irSelections
  }

  private fun FrontendIr.Selection.FragmentSpread.mergeWith(other: FrontendIr.Selection.FragmentSpread): FrontendIr.Selection.FragmentSpread {
    val newCondition = FrontendIr.Condition.Or(setOf(condition, other.condition))
    return copy(
        condition = newCondition
    )
  }

  private fun FrontendIr.InlineFragmentDefinition.mergeFieldsAndInlineFragments(): FrontendIr.InlineFragmentDefinition {
    return copy(
        selections = selections.mergeFieldsAndInlineFragments()
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.mergeFieldsAndInlineFragments(): FrontendIr.NamedFragmentDefinition {
    return copy(
        selections = selections.mergeFieldsAndInlineFragments()
    )
  }

  private fun FrontendIr.Selection.Field.mergeWith(other: FrontendIr.Selection.Field): FrontendIr.Selection.Field {
    return if (other.condition == condition) {
      copy(
          selections = (selections + other.selections).mergeFieldsAndInlineFragments()
      )
    } else {
      /**
       * If two fields are queried, the result is a "or"
       * {
       *   hero {
       *     name
       *     name @include(if: $condition)
       *   }
       * }
       */
      val newCondition = FrontendIr.Condition.Or(setOf(condition, other.condition))
      /**
       * If fields are of object type and have sub selections, we transfer that condition to sub selections
       * {
       *   hero {
       *     friend @include(if: $condition1) {
       *       name
       *     }
       *     friend @include(if: $condition2) {
       *       id
       *     }
       *   }
       * }
       *
       * becomes
       *
       * {
       *   hero {
       *     # The '||' below is not GraphQL compliant but is there to give an idea of what's happening
       *     friend @include(if: $condition1) || @include(if: $condition2) {
       *       name @include(if: $condition1)
       *       id @include(if: $condition2)
       *     }
       *   }
       * }
       *
       */
      copy(
          condition = newCondition,
          selections = (selections.addCondition(condition) + other.selections.addCondition(other.condition)).mergeFieldsAndInlineFragments()
      )
    }
  }

  /**
   * See [FrontendIr.Selection.Field.mergeWith] for more comments
   */
  private fun FrontendIr.Selection.InlineFragment.mergeWith(other: FrontendIr.Selection.InlineFragment): FrontendIr.Selection.InlineFragment {
    return if (other.condition == condition) {
      copy(
          fragmentDefinition = fragmentDefinition.mergeWith(other.fragmentDefinition)
      )
    } else {
      val newCondition = FrontendIr.Condition.Or(setOf(condition, other.condition))

      copy(
          condition = newCondition,
          fragmentDefinition = fragmentDefinition.addCondition(condition).mergeWith(other.fragmentDefinition.addCondition(other.condition))
      )
    }
  }

  private fun FrontendIr.InlineFragmentDefinition.mergeWith(other: FrontendIr.InlineFragmentDefinition): FrontendIr.InlineFragmentDefinition {
    return copy(
        selections = (selections + other.selections).mergeFieldsAndInlineFragments()
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.mergeWith(other: FrontendIr.NamedFragmentDefinition): FrontendIr.NamedFragmentDefinition {
    return copy(
        selections = (selections + other.selections).mergeFieldsAndInlineFragments()
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.addCondition(condition: FrontendIr.Condition): FrontendIr.NamedFragmentDefinition {
    return copy(
        selections = selections.addCondition(condition)
    )
  }

  private fun FrontendIr.InlineFragmentDefinition.addCondition(condition: FrontendIr.Condition): FrontendIr.InlineFragmentDefinition {
    return copy(
        selections = selections.addCondition(condition)
    )
  }

  /**
   * Applies a parent condition to children selections
   */
  private fun List<FrontendIr.Selection>.addCondition(condition: FrontendIr.Condition): List<FrontendIr.Selection> {
    return map {
      when (it) {
        is FrontendIr.Selection.Field -> {
          it.copy(
              condition = FrontendIr.Condition.And(setOf(condition, it.condition))
          )
        }
        is FrontendIr.Selection.FragmentSpread -> {
          it.copy(
              condition = FrontendIr.Condition.And(setOf(condition, it.condition))
          )
        }
        is FrontendIr.Selection.InlineFragment -> {
          it.copy(
              condition = FrontendIr.Condition.And(setOf(condition, it.condition))
          )
        }
      }
    }
  }

  private fun GQLInlineFragment.toIr(): FrontendIr.Selection.InlineFragment {
    val typeDefinition = schema.typeDefinition(typeCondition.name)
    val definition = FrontendIr.InlineFragmentDefinition(
        selections = selectionSet.toIr(typeDefinition),
        typeCondition = typeDefinition
    )

    return FrontendIr.Selection.InlineFragment(fragmentDefinition = definition, condition = directives.toCondition())
  }

  private fun GQLFragmentSpread.toIr(): FrontendIr.Selection.FragmentSpread {
    return FrontendIr.Selection.FragmentSpread(name = name, condition = directives.toCondition())
  }

  private fun List<GQLDirective>.toCondition(): FrontendIr.Condition {
    val conditions = mapNotNull {
      it.toCondition()
    }
    return if (conditions.isEmpty()) {
      FrontendIr.Condition.True
    } else {
      check(conditions.toSet().size == conditions.size) {
        "ApolloGraphQL: duplicate @skip/@include directives are not allowed"
      }
      FrontendIr.Condition.And(conditions.toSet())
    }
  }

  private fun GQLDirective.toCondition(): FrontendIr.Condition? {
    if (setOf("skip", "include").contains(name).not()) {
      // not a condition directive
      return null
    }
    if (arguments?.arguments?.size != 1) {
      throw IllegalStateException("ApolloGraphQL: wrong number of arguments for '$name' directive: ${arguments?.arguments?.size}")
    }

    val argument = arguments.arguments.first()

    return when (val value = argument.value) {
      is GQLBooleanValue -> {
        if (value.value) FrontendIr.Condition.True else FrontendIr.Condition.False
      }
      is GQLVariableValue -> FrontendIr.Condition.Variable(
          name = value.name,
          inverted = name == "skip"
      )
      else -> throw IllegalStateException("ApolloGraphQL: cannot pass ${value.toUtf8()} to '$name' directive")
    }
  }

  private fun String.toIr(): OperationType {
    return when (this) {
      "query" -> OperationType.Query
      "mutation" -> OperationType.Mutation
      "subscription" -> OperationType.Subscription
      else -> throw IllegalStateException("ApolloGraphQL: unknown operationType $this.")
    }
  }

  enum class OperationType {
    Query,
    Mutation,
    Subscription
  }
}
