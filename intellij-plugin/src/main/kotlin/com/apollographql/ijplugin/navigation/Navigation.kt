package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.psi.GraphQLDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLField
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentSpread
import com.intellij.lang.jsgraphql.psi.GraphQLInlineFragment
import com.intellij.lang.jsgraphql.psi.GraphQLOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLTypeCondition
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

private val APOLLO_OPERATION_TYPES = setOf(
    "com.apollographql.apollo3.api.Query",
    "com.apollographql.apollo3.api.Mutation",
    "com.apollographql.apollo3.api.Subscription",
)

private val APOLLO_FRAGMENT_TYPE = "com.apollographql.apollo3.api.Fragment.Data"

fun KtNameReferenceExpression.isApolloOperationOrFragmentReference(): Boolean {
  val resolvedElement = references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve()
  return (resolvedElement as? KtClass
      ?: (resolvedElement as? KtConstructor<*>)?.containingClass())
      ?.implementsOperationOrFragment() == true
}

fun KtNameReferenceExpression.isApolloModelField(): Boolean {
  return (references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve() as? KtParameter)
      ?.topMostContainingClass()
      ?.implementsOperationOrFragment() == true
}

private fun KtClass.implementsOperationOrFragment(): Boolean {
  return toLightClass()
      ?.implementsList
      ?.referencedTypes
      ?.any { classType ->
        classType.canonicalText.startsWith(APOLLO_FRAGMENT_TYPE) ||
            APOLLO_OPERATION_TYPES.any { classType.canonicalText.startsWith(it) }
      } == true
}

private fun KtElement.topMostContainingClass(): KtClass? {
  return if (containingClass() == null) {
    this as? KtClass
  } else {
    containingClass()!!.topMostContainingClass()
  }
}

fun findOperationOrFragmentGraphQLDefinitions(project: Project, name: String): List<GraphQLDefinition> {
  val definitions = mutableListOf<GraphQLDefinition>()
  val virtualFiles = FileTypeIndex.getFiles(GraphQLFileType.INSTANCE, GlobalSearchScope.allScope(project))
  for (virtualFile in virtualFiles) {
    val graphQLFile = PsiManager.getInstance(project).findFile(virtualFile) as GraphQLFile? ?: continue
    definitions +=
        // Look for operation definitions
        graphQLFile.findChildrenOfType<GraphQLOperationDefinition>()
            .filter { it.name == name || it.name == name.minusOperationTypeSuffix() }
            .ifEmpty {
              // Fallback: look for fragment definitions
              graphQLFile.findChildrenOfType<GraphQLFragmentDefinition> { it.name == name }
            }
  }
  return definitions
}

fun findGraphQLFields(nameReferenceExpression: KtNameReferenceExpression): List<GraphQLElement> {
  val fields = mutableListOf<GraphQLElement>()
  val project = nameReferenceExpression.project
  val containingClassName = (nameReferenceExpression.references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve() as? KtElement)
      ?.topMostContainingClass()
      ?.name
      ?: return emptyList()
  val operationOrFragmentDefinitions = findOperationOrFragmentGraphQLDefinitions(project, containingClassName)
  for (operationOrFragmentDefinition in operationOrFragmentDefinitions) {
    fields +=
        // Fields (including aliases)
        operationOrFragmentDefinition.findChildrenOfType<GraphQLField> { field ->
          field.name == nameReferenceExpression.text ||
              field.alias?.identifier?.referenceName == nameReferenceExpression.text
        } +
            // Fragment spreads
            operationOrFragmentDefinition.findChildrenOfType<GraphQLFragmentSpread> { fragmentSpread ->
              fragmentSpread.name.equals(nameReferenceExpression.text, ignoreCase = true)
            } +
            // Inline fragments
            operationOrFragmentDefinition.findChildrenOfType<GraphQLInlineFragment> { inlineFragment ->
              inlineFragment.typeCondition?.typeName?.name?.let { "on$it" } == nameReferenceExpression.text
            }
                // Only keep the type condition
                .flatMap { inlineFragment ->
                  inlineFragment.findChildrenOfType<GraphQLTypeCondition>()
                }
  }
  return fields
}

// Remove "Query", "Mutation", or "Subscription" from the end of the operation name
private fun String.minusOperationTypeSuffix(): String {
  return when {
    endsWith("Query") -> substringBeforeLast("Query")
    endsWith("Mutation") -> substringBeforeLast("Mutation")
    endsWith("Subscription") -> substringBeforeLast("Subscription")
    else -> this
  }
}
