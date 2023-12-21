@file:OptIn(ApolloInternal::class)

package com.apollographql.ijplugin.inspection

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLDefinition
import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.NULLABILITY_VERSION
import com.apollographql.apollo3.ast.nullabilityDefinitions
import com.apollographql.ijplugin.util.quoted
import com.apollographql.ijplugin.util.unquoted
import com.intellij.lang.jsgraphql.psi.GraphQLArrayValue
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLNamedElement
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLSchemaExtension
import com.intellij.lang.jsgraphql.psi.GraphQLStringValue

const val NULLABILITY_URL = "https://specs.apollo.dev/nullability/$NULLABILITY_VERSION"

val NULLABILITY_DEFINITIONS: List<GQLDefinition> by lazy {
  nullabilityDefinitions(NULLABILITY_VERSION)
}

val NULLABILITY_DIRECTIVE_DEFINITIONS: List<GQLDirectiveDefinition> by lazy {
  NULLABILITY_DEFINITIONS.filterIsInstance<GQLDirectiveDefinition>()
}

const val CATCH = "catch"

fun GraphQLNamedElement.isImported(): Boolean {
  for (schemaFile in schemaFiles()) {
    if (schemaFile.hasImportFor(this.name!!, this is GraphQLDirective)) return true
  }
  return false
}

fun isImported(element: GraphQLElement, enumName: String): Boolean {
  for (schemaFile in element.schemaFiles()) {
    if (schemaFile.hasImportFor(enumName, false)) return true
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

private fun GraphQLFile.hasImportFor(name: String, isDirective: Boolean): Boolean {
  for (directive in linkDirectives()) {
    val importArgValue = directive.argumentValue("import") as? GraphQLArrayValue
    if (importArgValue == null) {
      // Default import is everything - see https://specs.apollo.dev/link/v1.0/#@link.url
      val asArgValue = directive.argumentValue("as") as? GraphQLStringValue
      // Default prefix is the name part of the url
      val prefix = (asArgValue?.text?.unquoted() ?: "nullability") + "__"
      if (name.startsWith(prefix)) return true
    } else {
      if (importArgValue.valueList.any { it.text == name.nameForImport(isDirective).quoted() }) {
        return true
      }
    }
  }
  return false
}

val String.nameWithoutPrefix get() = substringAfter("__")

val GraphQLNamedElement.nameWithoutPrefix get() = name!!.nameWithoutPrefix

fun String.nameForImport(isDirective: Boolean) = "${if (isDirective) "@" else ""}${this.nameWithoutPrefix}"

val GraphQLNamedElement.nameForImport get() = if (this is GraphQLDirective) "@$nameWithoutPrefix" else nameWithoutPrefix
