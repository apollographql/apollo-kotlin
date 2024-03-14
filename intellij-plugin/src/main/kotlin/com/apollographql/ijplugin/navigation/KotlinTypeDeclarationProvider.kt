package com.apollographql.ijplugin.navigation

import com.apollographql.ijplugin.project.apolloProjectService
import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.lang.jsgraphql.psi.GraphQLNamedElement
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement

/**
 * Allows to navigate to the corresponding GraphQL type definition when shift-middle-clicking/shift-cmd-clicking/shift-cmd-b on a Kotlin
 * symbol whose type is an Apollo enum, input type or model field.
 */
class KotlinTypeDeclarationProvider : TypeDeclarationProvider {
  override fun getSymbolTypeDeclarations(symbol: PsiElement): Array<PsiElement>? {
    if (!symbol.project.apolloProjectService.apolloVersion.isAtLeastV3) return null

    // Only care about Kotlin
    if (symbol !is KtElement) return null

    // Get the original declaration(s)
    val ktTypeDeclarations = TypeDeclarationProvider.EP_NAME.extensionList.filterNot { it is KotlinTypeDeclarationProvider }
        .map { it.getSymbolTypeDeclarations(symbol) }.filterNotNull().firstOrNull()
    val ktTypeDeclaration = ktTypeDeclarations?.firstOrNull() ?: return null

    val gqlElements = when {
      ktTypeDeclaration is KtClass && ktTypeDeclaration.isApolloOperationOrFragment() -> {
        findOperationOrFragmentGraphQLDefinitions(ktTypeDeclaration.project, ktTypeDeclaration.name!!)
      }

      ktTypeDeclaration is KtClass && ktTypeDeclaration.isApolloEnumClass() -> {
        findEnumTypeGraphQLDefinitions(ktTypeDeclaration.project, ktTypeDeclaration.name!!)
      }

      ktTypeDeclaration is KtClass && ktTypeDeclaration.isApolloInputClass() -> {
        findInputTypeGraphQLDefinitions(ktTypeDeclaration.project, ktTypeDeclaration.name!!)
      }

      // For model fields, we want to navigate to the type declaration in the schema
      symbol.isApolloModelField() -> {
        findGraphQLElements(symbol).mapNotNull {
          (it as? GraphQLNamedElement)?.nameIdentifier?.reference?.resolve()
        }
      }

      else -> return null
    }

    return buildList {
      // Add GraphQL definition(s)
      addAll(gqlElements)

      // Add the original Kotlin declaration(s)
      addAll(ktTypeDeclarations)
    }.toTypedArray()
  }
}
