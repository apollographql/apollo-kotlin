package com.apollographql.ijplugin.icons

import com.intellij.ide.IconProvider
import com.intellij.lang.jsgraphql.psi.GraphQLElement
import com.intellij.psi.PsiElement
import javax.swing.Icon

class GraphQLIconProvider : IconProvider() {
  override fun getIcon(element: PsiElement, flags: Int): Icon? {
    if (element is GraphQLElement) {
      return ApolloIcons.Symbol.GraphQL
    }
    return null
  }
}
