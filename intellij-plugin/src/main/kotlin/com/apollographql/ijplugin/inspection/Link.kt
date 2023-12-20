@file:OptIn(ApolloInternal::class)

package com.apollographql.ijplugin.inspection

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.NULLABILITY_VERSION
import com.apollographql.apollo3.ast.nullabilityDefinitions
import com.apollographql.ijplugin.util.quoted
import com.apollographql.ijplugin.util.unquoted
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLNamedElement
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaExtension
import com.intellij.lang.jsgraphql.psi.GraphQLStringValue

const val NULLABILITY_URL = "https://specs.apollo.dev/nullability/$NULLABILITY_VERSION"

val NULLABILITY_DIRECTIVES: Map<String, Collection<String>> by lazy {
  nullabilityDefinitions(NULLABILITY_VERSION)
      .filterIsInstance<GQLDirectiveDefinition>()
      .map { it.name to it.arguments.map { it.name } }
      .toMap()
}

fun GraphQLNamedElement.isImported(): Boolean {
  for (schemaFile in schemaFiles()) {
    if (schemaFile.hasImportFor(this)) return true
  }
  return false
}

fun GraphQLFile.linkDirectives(): List<GraphQLDirective> {
  val schemaDirectives = typeDefinitions.filterIsInstance<GraphQLSchemaExtension>().flatMap { it.directives } +
      typeDefinitions.filterIsInstance<GraphQLSchemaDefinition>().flatMap { it.directives }
  return schemaDirectives.filter { directive ->
    directive.name == "link" &&
        directive.arguments?.argumentList.orEmpty().any { arg -> arg.name == "url" && arg.value?.text == NULLABILITY_URL.quoted() }
  }
}

private fun GraphQLFile.hasImportFor(element: GraphQLNamedElement): Boolean {
  for (directive in linkDirectives()) {
    val importArgValue = directive.arguments?.argumentList.orEmpty().firstOrNull { it.name == "import" }?.value as? GraphQLArrayValue
    if (importArgValue == null) {
      // Default import is everything - see https://specs.apollo.dev/link/v1.0/#@link.url
      val asArgValue = directive.arguments?.argumentList.orEmpty().firstOrNull { it.name == "as" }?.value as? GraphQLStringValue
      // Default prefix is the name part of the url
      val prefix = (asArgValue?.text?.unquoted() ?: "nullability") + "__"
      if (element.name!!.startsWith(prefix)) return true
    } else {
      if (importArgValue.valueList.any { it.text == element.nameForImport.quoted() }) {
        return true
      }
    }
  }
  return false
}

val GraphQLNamedElement.nameForImport get() = if (this is GraphQLDirective) "@" + name!! else name!!
