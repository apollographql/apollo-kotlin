package com.apollographql.ijplugin.inspection

import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.linkDefinitions
import com.apollographql.ijplugin.util.KOTLIN_LABS_DEFINITIONS
import com.apollographql.ijplugin.util.NULLABILITY_DEFINITIONS
import com.apollographql.ijplugin.util.NULLABILITY_URL
import com.apollographql.ijplugin.util.directives
import com.apollographql.ijplugin.util.isImported
import com.apollographql.ijplugin.util.nameWithoutPrefix
import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.lang.jsgraphql.psi.GraphQLArgument
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLDirectivesAware
import com.intellij.psi.PsiElement

private val KNOWN_DIRECTIVES: List<GQLDirectiveDefinition> by lazy {
  linkDefinitions().directives() + NULLABILITY_DEFINITIONS.directives() + KOTLIN_LABS_DEFINITIONS.directives()
}

/**
 * Do not highlight certain known directives as unresolved references.
 *
 * Note: we'll need this workaround until there is a way for a plugin to provide their own known definitions to the GraphQL plugin.
 * See https://github.com/JetBrains/js-graphql-intellij-plugin/issues/697.
 */
class GraphQLUnresolvedReferenceInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    val parent = element.parent ?: return false
    return when (toolId) {
      "GraphQLUnresolvedReference" -> parent.isKnownDirective() || parent.isKnownDirectiveArgument()

      "GraphQLMissingType" -> element is GraphQLDirectivesAware && element.directives.all { it.isKnownDirective() }

      // We need to suppress this one too because the plugin doesn't know that certain directives (e.g. @link) are repeatable
      "GraphQLDuplicateDirective" -> element is GraphQLDirective &&
          KNOWN_DIRECTIVES.any { it.name == element.name && it.repeatable }

      else -> false
    }
  }

  override fun getSuppressActions(psiElement: PsiElement?, s: String): Array<SuppressQuickFix> = SuppressQuickFix.EMPTY_ARRAY
}

private fun PsiElement.isKnownDirective(): Boolean {
  return this is GraphQLDirective && (name in KNOWN_DIRECTIVES.map { it.name } || this.isImported(NULLABILITY_URL))
}

private fun PsiElement.isKnownDirectiveArgument(): Boolean {
  return this is GraphQLArgument &&
      parent?.parent?.isKnownDirective() == true &&
      name in KNOWN_DIRECTIVES.firstOrNull { it.name == (parent.parent as GraphQLDirective).nameWithoutPrefix }?.arguments?.map { it.name }
      .orEmpty()
}
