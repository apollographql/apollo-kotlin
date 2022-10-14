@file:JvmName("-checkCapitalizedFields")

package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLField
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLFragmentSpread
import com.apollographql.apollo3.ast.GQLInlineFragment
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.GQLSelection
import com.apollographql.apollo3.ast.Issue
import com.apollographql.apollo3.ast.internal.IssuesScope

@ApolloInternal
fun checkCapitalizedFields(definitions: List<GQLDefinition>, checkFragmentsOnly: Boolean): List<Issue> {
  val scope = object : ValidationScope {
    override val issues = mutableListOf<Issue>()
    override val fragmentsByName = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
  }

  definitions.forEach { definition ->
    when {
      definition is GQLOperationDefinition && !checkFragmentsOnly -> scope.checkCapitalizedFields(definition.selectionSet.selections)
      definition is GQLFragmentDefinition -> scope.checkCapitalizedFields(definition.selectionSet.selections)
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
        val alias = it.alias
        if (alias != null) {
          if (isFirstLetterUpperCase(alias)) {
            issues.add(Issue.UpperCaseField(message = """
                      Capitalized alias '$alias' is not supported as it causes name clashes with the generated models. Use '${decapitalizeFirstLetter(alias)}' instead.
                    """.trimIndent(),
                sourceLocation = it.sourceLocation)
            )
          }
        } else if (isFirstLetterUpperCase(it.name)) {
          issues.add(Issue.UpperCaseField(message = """
                      Capitalized field '${it.name}' is not supported as it causes name clashes with the generated models. Use an alias instead or the 'flattenModels' or 'decapitalizeFields' compiler option.
                    """.trimIndent(),
              sourceLocation = it.sourceLocation)
          )
        }
        it.selectionSet?.let { selectionSet ->
          checkCapitalizedFields(selectionSet.selections)
        }
      }

      is GQLInlineFragment -> checkCapitalizedFields(it.selectionSet.selections)
      // it might be that the fragment is defined in an upstream module. In that case, it is validated
      // already, no need to check it again
      is GQLFragmentSpread -> fragmentsByName[it.name]?.let { fragment -> checkCapitalizedFields(fragment.selectionSet.selections) }
    }
  }
}

private interface ValidationScope : IssuesScope {
  override val issues: MutableList<Issue>
  val fragmentsByName: Map<String, GQLFragmentDefinition>
}

private fun isFirstLetterUpperCase(name: String): Boolean {
  return name.firstOrNull { it.isLetter() }?.isUpperCase() ?: true
}

private fun decapitalizeFirstLetter(name: String): String {
  val builder = StringBuilder(name.length)
  var isDecapitalized = false
  name.forEach {
    builder.append(if (!isDecapitalized && it.isLetter()) {
      isDecapitalized = true
      it.toString().lowercase()
    } else {
      it.toString()
    })
  }
  return builder.toString()
}
