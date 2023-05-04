package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.toUElement

/**
 * Allows to navigate to the corresponding GraphQL definition when middle-clicking/cmd-clicking/cmd-b on an Apollo operation/fragment
 * constructor call.
 */
class OperationAndFragmentGotoDeclarationHandler : GotoDeclarationHandler {
  override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor?): Array<PsiElement>? {
    if (sourceElement == null) return null
    if (!sourceElement.project.apolloProjectService.isApolloKotlin3Project) return null

    val uElement = sourceElement.toUElement()?.uastParent
    if (uElement !is UCallExpression || uElement.kind != UastCallKind.CONSTRUCTOR_CALL) return null
    if (!uElement.isApolloOperationOrFragment()) return null
    val psiLeaf = PsiTreeUtil.getDeepestFirst(sourceElement)
    return buildList {
      // Add GraphQL definition(s)
      addAll(findOperationOrFragmentGraphQLDefinition(sourceElement.project, psiLeaf.text))

      // Add the generated class
      uElement.resolve()?.containingClass?.let { add(it) }
    }.toTypedArray()
  }
}
