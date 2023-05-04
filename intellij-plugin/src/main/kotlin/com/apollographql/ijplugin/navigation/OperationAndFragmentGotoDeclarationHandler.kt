package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

/**
 * Allows to navigate to the corresponding GraphQL definition when middle-clicking/cmd-clicking/cmd-b on an Apollo operation/fragment
 * constructor call.
 */
class OperationAndFragmentGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
    if (sourceElement == null) return null
    if (!sourceElement.project.apolloProjectService.isApolloKotlin3Project) return null

    val nameReferenceExpression = sourceElement.parent as? KtNameReferenceExpression ?: return null
    if (!nameReferenceExpression.isApolloOperationOrFragment()) return null
    val psiLeaf = PsiTreeUtil.getDeepestFirst(sourceElement)
    return buildList {
      // Add GraphQL definition(s)
      addAll(findOperationOrFragmentGraphQLDefinition(sourceElement.project, psiLeaf.text))

      // Add the generated class
      val containingClass = (nameReferenceExpression.references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve() as? KtFunction)?.containingClass()
      if (containingClass != null) {
        add(containingClass)
      }
    }.toTypedArray()
  }
}
