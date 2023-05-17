package com.apollographql.ijplugin.navigation

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.codeInsight.KotlinTypeDeclarationProvider
import org.jetbrains.kotlin.psi.KtElement

/**
 * Allows to navigate to the corresponding GraphQL type definition when shift-middle-clicking/shift-cmd-clicking/shift-cmd-b on an Apollo
 * operation/fragment/enum reference, or model field.
 *
 * TODO not implemented yet
 */
class GraphQLTypeDeclarationProvider : TypeDeclarationProvider {
  override fun getSymbolTypeDeclarations(symbol: PsiElement): Array<PsiElement>? {
    // Only care about Kotlin
    if (symbol !is KtElement) return null

    return buildList {
      // Add GraphQL definition(s)
      // TODO add(...)

      // Add the original referred to element
      TypeDeclarationProvider.EP_NAME.getExtensionList().firstOrNull { it is KotlinTypeDeclarationProvider }
          ?.getSymbolTypeDeclarations(symbol)
          ?.let { addAll(it) }
    }.toTypedArray()
  }
}
