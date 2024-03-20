package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.project.apolloProjectService
import com.apollographql.ijplugin.telemetry.TelemetryEvent
import com.apollographql.ijplugin.telemetry.telemetryService
import com.apollographql.ijplugin.util.originalClassName
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.getPsiElementPopup
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.ide.util.EditSourceUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Adds a gutter icon to Apollo operation/fragment references in Kotlin code, allowing to navigate to the corresponding GraphQL definition.
 */
class KotlinDefinitionMarkerProvider : RelatedItemLineMarkerProvider() {

  override fun getName() = ApolloBundle.message("navigation.GraphQLDefinitionMarkerProvider.name")

  override fun getIcon() = ApolloIcons.Gutter.GraphQL

  override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
    if (!element.project.apolloProjectService.apolloVersion.isAtLeastV3) return

    val nameReferenceExpression = element as? KtNameReferenceExpression ?: return
    val psiLeaf = PsiTreeUtil.getDeepestFirst(element)
    val graphQLDefinitions = when {
      nameReferenceExpression.isApolloOperationOrFragmentReference() -> {
        findOperationOrFragmentGraphQLDefinitions(element.project, nameReferenceExpression.originalClassName() ?: psiLeaf.text)
      }

      else -> return
    }

    if (graphQLDefinitions.isEmpty()) return
    val markerInfo = NavigationGutterIconBuilder.create(ApolloIcons.Gutter.GraphQL)
        .setTargets(graphQLDefinitions)
        .setTooltipText(ApolloBundle.message("navigation.GraphQLDefinitionMarkerProvider.tooltip", psiLeaf.text))
        .createLineMarkerInfo(psiLeaf) { e, _ ->
          // This behavior is already handled by NavigationGutterIcon, but we re-implement it here in order to log the telemetry event
          element.project.telemetryService.logEvent(TelemetryEvent.ApolloIjMarkerToGraphQl())
          if (graphQLDefinitions.size == 1) {
            graphQLDefinitions[0].navigate()
          } else {
            val popup = getPsiElementPopup(graphQLDefinitions.toTypedArray(), DefaultPsiElementCellRenderer(), "") { element: PsiElement ->
              element.navigate()
              true
            }
            popup.show(RelativePoint(e))
          }
        }
    result.add(markerInfo)
  }
}

private fun PsiElement.navigate() {
  val descriptor = EditSourceUtil.getDescriptor(this)
  if (descriptor != null && descriptor.canNavigate()) {
    descriptor.navigate(true)
  }
}
