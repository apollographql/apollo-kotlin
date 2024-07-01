package com.apollographql.apollo.ast.internal.validation

import com.apollographql.apollo.ast.DuplicateDeferLabel
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.InvalidDeferDirective
import com.apollographql.apollo.ast.InvalidDeferLabel
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.SourceLocation
import com.apollographql.apollo.ast.VariableDeferLabel
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.responseName

private class Scope(
    val schema: Schema,
    val fragments: Map<String, GQLFragmentDefinition>,
) {
  val issues = mutableListOf<Issue>()
  private val deferDirectiveLabels = mutableMapOf<String, SourceLocation?>()
  private val deferDirectivePathAndLabels = mutableMapOf<String, SourceLocation?>()

  fun validate(operation: GQLOperationDefinition, parentType: String): List<Issue> {
    operation.selections.validate(parentType, emptyList(), operation, emptySet())

    return issues
  }

  private fun List<GQLSelection>.validate(
      parentType: String,
      path: List<String>,
      operation: GQLOperationDefinition?,
      visitedFragmentSpreads: Set<String>,
  ) {
    forEach {
      when (it) {
        is GQLField -> {
          val typeDefinition = schema.typeDefinitions.get(parentType) ?: return@forEach
          val fieldDefinition = it.definitionFromScope(schema, typeDefinition) ?: return@forEach
          it.selections.validate(fieldDefinition.type.rawType().name, path + it.responseName(), null, visitedFragmentSpreads)
        }

        is GQLFragmentSpread -> {
          it.directives.validate(operation, path)

          if (visitedFragmentSpreads.contains(it.name)) {
            // This fragment is cyclic
            return@forEach
          }
          val fragmentDefinition = fragments.get(it.name) ?: return@forEach
          fragmentDefinition.selections.validate(fragmentDefinition.typeCondition.name, path, null, visitedFragmentSpreads + it.name)
        }

        is GQLInlineFragment -> {
          it.directives.validate(operation, path)
          it.selections.validate(it.typeCondition?.name ?: parentType, path, null, visitedFragmentSpreads)
        }
      }
    }
  }

  private fun List<GQLDirective>.validate(operation: GQLOperationDefinition?, path: List<String>) {
    forEach {
      if (it.name == "defer") {
        it.validateDeferDirective(operation, path)
      }
    }
  }

  /**
   * - If a label is passed to a `@defer` directive, it must not be a variable, and it must be unique within all other `@defer` directives in
   * the document.
   * - The @defer directive is not allowed to be used on root fields of the mutation or subscription type.
   * - Check that the label can be used as part of an identifier name (Apollo-specific validation).
   * - Check that any `@defer` directive found when walking fragments on an operation have a unique path + label (Apollo-specific
   * validation). For instance: this is invalid:
   * ```
   * query Query1 {
   *   computers {
   *   ...ComputerFields @defer  # path is computer
   *   ...ComputerFields @defer  # path is computer
   *   }
   * }
   * ```
   *
   * Also invalid:
   * ```
   * query Query2 {
   *   computers {
   *     ...ComputerFields
   *     ...ComputerFields2
   *     }
   *   }
   *
   *   fragment ComputerFields on Computer {
   *     screen {
   *       ...ScreenFields @defer  # path is computer.screen
   *     }
   *   }
   *
   *   fragment ComputerFields2 on Computer {
   *     screen {
   *       ...ScreenFields @defer  # path is computer.screen
   *     }
   * }
   * ```
   */
  private fun GQLDirective.validateDeferDirective(parent: GQLOperationDefinition?, path: List<String>) {
    val label = arguments.firstOrNull { it.name == "label" }?.value
    if (label is GQLVariableValue) {
      issues.add(
          VariableDeferLabel(
              message = "@defer label argument must not be a variable",
              sourceLocation = sourceLocation
          )
      )
      return
    }

    if (parent != null && (parent.operationType == "mutation" || parent.operationType == "subscription")) {
      issues.add(
          InvalidDeferDirective(
              message = "The @defer directive is not allowed to be used on root fields of mutations or subscriptions",
              sourceLocation = sourceLocation
          )
      )
    }

    var labelStringValue = ""
    if (label != null) {
      // If label is not a GQLStringValue, prior validation will have issued an error already, so we can ignore this one
      if (label !is GQLStringValue) return
      labelStringValue = label.value

      // We use the label in part of the synthetic field's name in the generated model, so it needs to be a valid Kotlin/Java identifier
      if (!labelStringValue.matches(Regex("[a-zA-Z0-9_]+"))) {
        issues.add(
            InvalidDeferLabel(
                message = "@defer label '$labelStringValue' must only contain letters, numbers, or underscores",
                sourceLocation = sourceLocation
            )
        )
      }

      if (labelStringValue in deferDirectiveLabels) {
        issues.add(
            DuplicateDeferLabel(
                message = "@defer label '$labelStringValue' must be unique within all other @defer directives in the document. " +
                    "Same label found in ${deferDirectiveLabels[labelStringValue]!!.pretty()}",
                sourceLocation = sourceLocation
            )
        )
        return
      }
      deferDirectiveLabels[labelStringValue] = sourceLocation
    }

    val joinedPath = path.joinToString(".")
    val pathAndLabel = "$joinedPath/$labelStringValue"
    if (pathAndLabel in deferDirectivePathAndLabels) {
      val labelMessage = if (labelStringValue.isEmpty()) "no label" else "label '$labelStringValue'"
      issues.add(
          DuplicateDeferLabel(
              message = "A @defer directive with the same path '$joinedPath' and $labelMessage is already defined in ${deferDirectivePathAndLabels[pathAndLabel]!!.pretty()}. " +
                  "Set a unique label to distinguish them.",
              sourceLocation = sourceLocation
          )
      )
      return
    }
    deferDirectivePathAndLabels[pathAndLabel] = sourceLocation
  }
}

internal fun validateDeferLabels(
    operation: GQLOperationDefinition,
    parentType: String,
    schema: Schema,
    fragments: Map<String, GQLFragmentDefinition>,
): List<Issue> {
   return Scope(schema, fragments).validate(operation, parentType)
}
