package com.apollographql.ijplugin.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ApolloIcons {
  object Gutter {
    val GraphQL by lazy { load("/icons/gutter-graphql.svg") }
  }

  object Symbol {
    val GraphQL by lazy { load("/icons/symbol-graphql.svg") }
  }

  private fun load(path: String): Icon {
    return IconLoader.getIcon(path, ApolloIcons::class.java)
  }
}
