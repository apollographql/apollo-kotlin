package com.apollographql.ijplugin.inspection

import com.apollographql.apollo3.ast.GQLDirectiveDefinition
import com.apollographql.apollo3.ast.linkDefinitions
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
  linkDefinitions().directives() + NULLABILITY_DEFINITIONS.directives()
}

/**
 * Do not highlight certain known directives as unresolved references.
 *
 * TODO: remove this once https://github.com/JetBrains/js-graphql-intellij-plugin/pull/698 is merged.
 */
class GraphQLUnresolvedReferenceInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    val parent = element.parent ?: return false
    return when (toolId) {
      "GraphQLUnresolvedReference" -> parent.isKnownDirective() || parent.isKnownDirectiveArgument()

      "GraphQLMissingType" -> element is GraphQLDirectivesAware && element.directives.all { it.isKnownDirective() }

      // We need to suppress this one too because the plugin doesn't know that @link is repeatable
      "GraphQLDuplicateDirective" -> element is GraphQLDirective && element.name == "link"

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
      name in KNOWN_DIRECTIVES.firstOrNull { it.name == (parent.parent as GraphQLDirective).nameWithoutPrefix }?.arguments?.map { it.name }.orEmpty()
}
