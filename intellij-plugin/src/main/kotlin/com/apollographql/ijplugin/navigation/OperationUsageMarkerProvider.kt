package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.toUElement

private val APOLLO_OPERATION_TYPES = arrayOf(
    "com.apollographql.apollo3.api.Query",
    "com.apollographql.apollo3.api.Mutation",
    "com.apollographql.apollo3.api.Subscription"
)

/**
 * Adds a gutter icon to Apollo operation usage in Kotlin/Java, allowing to navigate to the corresponding operation GraphQL definition.
 *
 * TODO: for now the icon appears but navigation to the definition is not yet implemented.
 */
class OperationUsageMarkerProvider : RelatedItemLineMarkerProvider() {
  private val gutterIcon by lazy { IconLoader.getIcon("/icons/gutter-operation.svg", this::class.java) }

  override fun collectNavigationMarkers(
      element: PsiElement,
      result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
  ) {
    val apolloProjectService = element.project.apolloProjectService
    if (!apolloProjectService.isApolloKotlin3Project) return

    val uElement = element.toUElement()
    if (uElement !is UCallExpression || uElement.kind != UastCallKind.CONSTRUCTOR_CALL) return

    val isApolloOperation = (uElement.resolve() as? PsiMember)
        ?.containingClass
        ?.implementsList
        ?.referencedTypes
        ?.any { classType ->
          classType.resolve()?.qualifiedName in APOLLO_OPERATION_TYPES
        } == true

    if (isApolloOperation) {
      val psiLeaf = PsiTreeUtil.getDeepestFirst(element)
      val builder =
          NavigationGutterIconBuilder.create(gutterIcon)
              .setTargets(element)
              .setTooltipText(ApolloBundle.message("navigation.operation.tooltip"))
              .createLineMarkerInfo(psiLeaf)
      result.add(builder)
    }
  }
}
