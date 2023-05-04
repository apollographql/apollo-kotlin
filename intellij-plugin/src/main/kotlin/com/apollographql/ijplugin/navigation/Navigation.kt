package com.apollographql.ijplugin.navigation

import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.psi.GraphQLDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLOperationDefinition
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMember
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UCallExpression

private val APOLLO_OPERATION_OR_FRAGMENT_TYPES = arrayOf(
    "com.apollographql.apollo3.api.Query",
    "com.apollographql.apollo3.api.Mutation",
    "com.apollographql.apollo3.api.Subscription",
    "com.apollographql.apollo3.api.Fragment.Data",
)

fun UCallExpression.isApolloOperationOrFragment() = (resolve() as? PsiMember)
    ?.containingClass
    ?.implementsList
    ?.referencedTypes
    ?.any { classType ->
      APOLLO_OPERATION_OR_FRAGMENT_TYPES.any { classType.canonicalText.startsWith(it) }
    } == true

fun findOperationOrFragmentGraphQLDefinition(project: Project, name: String): List<GraphQLDefinition> {
  val definitions = mutableListOf<GraphQLDefinition>()
  val virtualFiles = FileTypeIndex.getFiles(GraphQLFileType.INSTANCE, GlobalSearchScope.allScope(project))
  for (virtualFile in virtualFiles) {
    val graphQLFile = PsiManager.getInstance(project).findFile(virtualFile) as GraphQLFile? ?: continue
    definitions +=
        // Look for operation definitions
        PsiTreeUtil.findChildrenOfType(graphQLFile, GraphQLOperationDefinition::class.java)
            .filter { it.name == name || it.name == name.minusOperationTypeSuffix() }
            .ifEmpty {
              // Fallback: look for fragment definitions
              PsiTreeUtil.findChildrenOfType(graphQLFile, GraphQLFragmentDefinition::class.java)
                  .filter { it.name == name }
            }
  }
  return definitions
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
