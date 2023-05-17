package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.ApolloIcons
import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Adds a gutter icon to Apollo operation/fragment references allowing to navigate to the corresponding GraphQL definition.
 */
class GraphQLDefinitionMarkerProvider : RelatedItemLineMarkerProvider() {

  override fun getName() = ApolloBundle.message("navigation.GraphQLDefinitionMarkerProvider.name")

  override fun getIcon() = ApolloIcons.Gutter.GraphQL

  override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
    if (!element.project.apolloProjectService.isApolloKotlin3Project) return

    val nameReferenceExpression = element as? KtNameReferenceExpression ?: return
    val psiLeaf = PsiTreeUtil.getDeepestFirst(element)
    val graphQLDefinitions = when {
      nameReferenceExpression.isApolloOperationOrFragmentReference() -> {
        findOperationOrFragmentGraphQLDefinitions(element.project, psiLeaf.text)
      }

      else -> return
    }

    if (graphQLDefinitions.isEmpty()) return
    val builder = NavigationGutterIconBuilder.create(ApolloIcons.Gutter.GraphQL)
        .setTargets(graphQLDefinitions)
        .setTooltipText(ApolloBundle.message("navigation.GraphQLDefinitionMarkerProvider.tooltip", psiLeaf.text))
        .createLineMarkerInfo(psiLeaf)
    result.add(builder)
  }
}
