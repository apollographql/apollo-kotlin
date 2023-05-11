package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.util.findChildrenOfType
import com.intellij.lang.jsgraphql.GraphQLFileType
import com.intellij.lang.jsgraphql.psi.GraphQLDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.lang.jsgraphql.psi.GraphQLFile
import com.intellij.lang.jsgraphql.psi.GraphQLFragmentDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLOperationDefinition
import com.intellij.lang.jsgraphql.psi.GraphQLSelectionSet
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.base.utils.fqname.fqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.nj2k.postProcessing.type
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

private const val APOLLO_FRAGMENT_TYPE = "com.apollographql.apollo3.api.Fragment.Data"

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

fun findGraphQLElements(nameReferenceExpression: KtNameReferenceExpression): List<GraphQLElement> {
  val elements = mutableListOf<GraphQLElement>()
  val project = nameReferenceExpression.project
  val parameter = nameReferenceExpression.references.firstIsInstanceOrNull<KtSimpleNameReference>()?.resolve() as? KtParameter
      ?: return emptyList()
  val operationOrFragmentClass = parameter.topMostContainingClass() ?: return emptyList()
  val fieldPath = operationOrFragmentClass.parameterPath(parameter)
  val operationOrFragmentDefinitions = findOperationOrFragmentGraphQLDefinitions(project, operationOrFragmentClass.name!!)
  for (operationOrFragmentDefinition in operationOrFragmentDefinitions) {
    elements += operationOrFragmentDefinition.findElementAtPath(fieldPath) ?: continue
  }
  return elements
}

/**
 * For a given [parameter], return a path for this field in the given operation class.
 *
 * For instance if the operation class is:
 * ```
 * class MyQuery : Query {
 *   data class Data(val user: User)
 *                              ↖
 *                  data class User(val address: Address)
 *                                                ↖
 *                                    data class Address(val street: String)
 * }
 * ```
 *
 * Then the path for `Address.street` will be `["user", "address", "street"]`.
 */
private fun KtClass.parameterPath(parameter: KtParameter): List<String> {
  var modelClass = parameter.containingClass()!!
  val parameterPath = mutableListOf(parameter.name!!)
  constructPath@ while (true) {
    var found = false
    findClass@ for (candidateModelClass in findChildrenOfType<KtClass>()) {
      // Look for the parameter in the constructor
      for (ctorParameter in candidateModelClass.primaryConstructor?.valueParameters.orEmpty()) {
        val parameterType = ctorParameter.type()
        if (parameterType?.fqName == modelClass.fqName ||
            // For lists
            parameterType?.arguments?.firstOrNull()?.type?.fqName == modelClass.fqName
        ) {
          parameterPath.add(0, ctorParameter.name!!)
          modelClass = candidateModelClass
          found = true
          break@findClass
        }
      }
    }
    if (!found) break
  }
  return parameterPath
}

/**
 * Get the element (field, fragment spread, or inline fragment type condition) at the given path.
 */
private fun GraphQLDefinition.findElementAtPath(path: List<String>): GraphQLElement? {
  var element: GraphQLElement? = null
  var selectionSet = findChildrenOfType<GraphQLSelectionSet>(recursive = false).firstOrNull() ?: return null
  for (pathElement in path) {
    var found = false
    for (selection in selectionSet.selectionList) {
      if (selection.field != null) {
        // Field
        val field = selection.field!!
        if (field.name == pathElement ||
            field.alias?.identifier?.referenceName == pathElement) {
          element = field
          field.selectionSet?.let { selectionSet = it }
          found = true
          break
        }
      } else if (selection.fragmentSelection != null) {
        // Fragment
        if (selection.fragmentSelection!!.inlineFragment != null) {
          // Inline
          val inlineFragment = selection.fragmentSelection!!.inlineFragment!!
          if (inlineFragment.typeCondition?.typeName?.name?.let { "on$it" } == pathElement) {
            // Point to the type condition
            element = inlineFragment.typeCondition
            inlineFragment.selectionSet?.let { selectionSet = it }
            found = true
            break
          }
        } else if (selection.fragmentSelection!!.fragmentSpread != null) {
          // Spread
          val fragmentSpread = selection.fragmentSelection!!.fragmentSpread!!
          if (fragmentSpread.name.equals(pathElement, ignoreCase = true)) {
            element = fragmentSpread
            found = true
            break
          }
        }
      }
    }
    if (!found) {
      element = null
      break
    }
  }
  return element
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
