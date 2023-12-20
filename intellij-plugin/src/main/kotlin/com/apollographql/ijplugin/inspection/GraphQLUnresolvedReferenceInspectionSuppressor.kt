package com.apollographql.ijplugin.inspection

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.lang.jsgraphql.psi.GraphQLArgument
import com.intellij.lang.jsgraphql.psi.GraphQLDirective
import com.intellij.lang.jsgraphql.psi.GraphQLDirectivesAware
import com.intellij.psi.PsiElement

private val KNOWN_DIRECTIVES: Map<String, Collection<String>> by lazy {
  mapOf("link" to setOf("url", "as", "import", "for")) + NULLABILITY_DIRECTIVES
}

/**
 * Do not highlight certain known directives as unresolved references.
 *
 * TODO: remove this once https://github.com/JetBrains/js-graphql-intellij-plugin/pull/698 is merged.
 */
class GraphQLUnresolvedReferenceInspectionSuppressor : InspectionSuppressor {
  override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
    val parent = element.parent
    return when (toolId) {
      "GraphQLUnresolvedReference" -> parent.isKnownDirective() || parent.isKnownDirectiveArgument()

      "GraphQLMissingType" -> element is GraphQLDirectivesAware && element.directives.all { it.isKnownDirective() }

      else -> false
    }
  }

  override fun getSuppressActions(psiElement: PsiElement?, s: String): Array<SuppressQuickFix> = SuppressQuickFix.EMPTY_ARRAY
}

private fun PsiElement.isKnownDirective(): Boolean {
  return this is GraphQLDirective && (name in KNOWN_DIRECTIVES.keys || this.isImported())
}

private fun PsiElement.isKnownDirectiveArgument(): Boolean {
  return this is GraphQLArgument &&
      parent?.parent?.isKnownDirective() == true &&
      name in KNOWN_DIRECTIVES[(parent.parent as GraphQLDirective).nameWithoutPrefix].orEmpty()
}
