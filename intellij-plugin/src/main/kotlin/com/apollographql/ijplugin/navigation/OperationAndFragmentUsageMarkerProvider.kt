package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.ApolloBundle
import com.apollographql.ijplugin.ApolloIcons
import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.psi.GraphQLDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLOperationDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.toUElement

private val APOLLO_OPERATION_OR_FRAGMENT_TYPES = arrayOf(
    "com.apollographql.apollo3.api.Query",
    "com.apollographql.apollo3.api.Mutation",
    "com.apollographql.apollo3.api.Subscription",
    "com.apollographql.apollo3.api.Fragment.Data",
)

/**
 * Adds a gutter icon to Apollo operation/fragment usage in Kotlin/Java, allowing to navigate to the corresponding GraphQL definition.
 */
class OperationAndFragmentUsageMarkerProvider : RelatedItemLineMarkerProvider() {

  override fun getName() = ApolloBundle.message("navigation.OperationAndFragmentUsageMarkerProvider.name")

  override fun getIcon() = ApolloIcons.Gutter.GraphQL

  override fun collectNavigationMarkers(
      element: PsiElement,
      result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
  ) {
    val apolloProjectService = element.project.apolloProjectService
    if (!apolloProjectService.isApolloKotlin3Project) return

    val uElement = element.toUElement()
    if (uElement !is UCallExpression || uElement.kind != UastCallKind.CONSTRUCTOR_CALL) return

    val isApolloOperationOrFragment = (uElement.resolve() as? PsiMember)
        ?.containingClass
        ?.implementsList
        ?.referencedTypes
        ?.any { classType ->
          classType.resolve()?.qualifiedName in APOLLO_OPERATION_OR_FRAGMENT_TYPES
        } == true

    if (isApolloOperationOrFragment) {
      val psiLeaf = PsiTreeUtil.getDeepestFirst(element)
      val builder =
          NavigationGutterIconBuilder.create(ApolloIcons.Gutter.GraphQL)
              .setTargets(findGraphQLDefinition(element.project, psiLeaf.text))
              .setTooltipText(ApolloBundle.message("navigation.operation.tooltip"))
              .createLineMarkerInfo(psiLeaf)
      result.add(builder)
    }
  }
}

private fun findGraphQLDefinition(project: Project, name: String): List<GraphQLDefinition> {
  val operationDefinitions = mutableListOf<GraphQLDefinition>()
  val virtualFiles = FileTypeIndex.getFiles(GraphQLFileType.INSTANCE, GlobalSearchScope.allScope(project))
  for (virtualFile in virtualFiles) {
    val graphQLFile = PsiManager.getInstance(project).findFile(virtualFile) as GraphQLFile? ?: continue
    operationDefinitions +=
        // Look for operation definitions
        PsiTreeUtil.findChildrenOfType(graphQLFile, GraphQLOperationDefinition::class.java)
            .filter { it.name == name || it.name == name.minusOperationTypeSuffix() }
            .ifEmpty {
              // Fallback: look for fragment definitions
              PsiTreeUtil.findChildrenOfType(graphQLFile, GraphQLFragmentDefinition::class.java)
                  .filter { it.name == name }
            }
  }
  return operationDefinitions
}

// Remove "Query", "Mutation" or "Subscription" from the end of the operation name
private fun String.minusOperationTypeSuffix(): String {
  return when {
    endsWith("Query") -> substringBeforeLast("Query")
    endsWith("Mutation") -> substringBeforeLast("Mutation")
    endsWith("Subscription") -> substringBeforeLast("Subscription")
    else -> this
  }
}
