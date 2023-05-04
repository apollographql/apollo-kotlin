package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.ApolloIcons
import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.toUElement

/**
 * Adds a gutter icon to Apollo operation/fragment constructor call allowing to navigate to the corresponding GraphQL definition.
 */
class OperationAndFragmentUsageMarkerProvider : RelatedItemLineMarkerProvider() {

  override fun getName() = ApolloBundle.message("navigation.OperationAndFragmentUsageMarkerProvider.name")

  override fun getIcon() = ApolloIcons.Gutter.GraphQL

  override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
    if (!element.project.apolloProjectService.isApolloKotlin3Project) return

    val uElement = element.toUElement()
    if (uElement !is UCallExpression || uElement.kind != UastCallKind.CONSTRUCTOR_CALL) return
    if (uElement.isApolloOperationOrFragment()) {
      val psiLeaf = PsiTreeUtil.getDeepestFirst(element)
      val builder = NavigationGutterIconBuilder.create(ApolloIcons.Gutter.GraphQL)
          .setTargets(findOperationOrFragmentGraphQLDefinition(element.project, psiLeaf.text))
          .setTooltipText(ApolloBundle.message("navigation.operation.tooltip"))
          .createLineMarkerInfo(psiLeaf)
      result.add(builder)
    }
  }
}
