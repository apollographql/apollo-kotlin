package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

/**
 * Allows to navigate to the corresponding GraphQL definition when middle-clicking/cmd-clicking/cmd-b on an Apollo operation/fragment
 * reference, or model field.
 */
class GraphQLGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
    if (sourceElement == null) return null
    if (!sourceElement.project.apolloProjectService.isApolloKotlin3Project) return null

    val nameReferenceExpression = sourceElement.parent as? KtNameReferenceExpression ?: return null
    return when {
      nameReferenceExpression.isApolloOperationOrFragmentReference() -> {
        val psiLeaf = PsiTreeUtil.getDeepestFirst(sourceElement)
        val graphQLDefinitions = findOperationOrFragmentGraphQLDefinitions(sourceElement.project, psiLeaf.text)
        if (graphQLDefinitions.isEmpty()) return null
        return buildList {
          // Add GraphQL definition(s)
          addAll(graphQLDefinitions)

          // Add the original referred to element
          val resolvedElement = nameReferenceExpression.references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve()
          if (resolvedElement != null) {
            add(resolvedElement)
          }
        }.toTypedArray()
      }

      nameReferenceExpression.isApolloModelField() -> {
        return buildList {
          // Add GraphQL field(s)
          addAll(findGraphQLElements(nameReferenceExpression))

          // Add the original referred to element
          val resolvedElement = nameReferenceExpression.references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve()
          if (resolvedElement != null) {
            add(resolvedElement)
          }
        }.toTypedArray()
      }

      else -> null
    }
  }
}
