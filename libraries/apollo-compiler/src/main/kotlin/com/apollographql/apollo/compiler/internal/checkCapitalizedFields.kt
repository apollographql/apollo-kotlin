@file:JvmName("-checkCapitalizedFields")

package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.Issue
import com.apollographql.apollo.ast.UpperCaseField

internal fun checkCapitalizedFields(definitions: List<GQLDefinition>, checkFragmentsOnly: Boolean): List<Issue> {
  val scope = object : ValidationScope {
    override val issues = mutableListOf<Issue>()
  }

  /**
   * Every fragment definition is checked here, on its own. Fragment spreads are therefore not
   * followed: the spread target is either one of these definitions, and already checked, or it
   * belongs to an upstream module and was checked when that module was compiled.
   */
  definitions.forEach { definition ->
    when {
      definition is GQLOperationDefinition && !checkFragmentsOnly -> scope.checkCapitalizedFields(definition.selections)
      definition is GQLFragmentDefinition -> scope.checkCapitalizedFields(definition.selections)
    }
  }

  return scope.issues
}

/**
 * Fields named with a capital first letter clash with the corresponding model name, unless flatten.
 */
private fun ValidationScope.checkCapitalizedFields(selections: List<GQLSelection>) {
  selections.forEach {
    when (it) {
      is GQLField -> {
        if (it.selections.isEmpty()) {
          // A clash can only occur if a model is generated
          return@forEach
        }
        val alias = it.alias
        if (alias != null) {
          if (isFirstLetterUpperCase(alias)) {
            issues.add(
                UpperCaseField(
                    message = """
                      Capitalized alias '$alias' is not supported as it causes name clashes with the generated models. Use '${decapitalizeFirstLetter(alias)}' instead.
                    """.trimIndent(),
                    sourceLocation = it.sourceLocation
                )
            )
          }
        } else if (isFirstLetterUpperCase(it.name)) {
          issues.add(
              UpperCaseField(
                  message = """
                    Capitalized field '${it.name}' is not supported as it causes name clashes with the generated models. Use an alias instead or the 'flattenModels' or 'decapitalizeFields' compiler option.
                  """.trimIndent(),
                  sourceLocation = it.sourceLocation
              )
          )
        }
        checkCapitalizedFields(it.selections)
      }

      is GQLInlineFragment -> checkCapitalizedFields(it.selections)
      // The spread target is checked on its own: either it is one of the definitions checked
      // above, or it is defined in an upstream module and was validated there.
      is GQLFragmentSpread -> Unit
    }
  }
}

private interface ValidationScope {
  val issues: MutableList<Issue>
}

private fun isFirstLetterUpperCase(name: String): Boolean {
  return name.firstOrNull { it.isLetter() }?.isUpperCase() ?: true
}

private fun decapitalizeFirstLetter(name: String): String {
  val builder = StringBuilder(name.length)
  var isDecapitalized = false
  name.forEach {
    builder.append(
        if (!isDecapitalized && it.isLetter()) {
          isDecapitalized = true
          it.toString().lowercase()
        } else {
          it.toString()
        }
    )
  }
  return builder.toString()
}
