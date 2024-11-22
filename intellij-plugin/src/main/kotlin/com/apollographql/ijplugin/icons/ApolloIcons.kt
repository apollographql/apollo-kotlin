package com.apollographql.ijplugin.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ApolloIcons {
  object Gutter {
    val GraphQL by lazy { load("/icons/gutter-graphql.svg") }
  }

  object Symbol {
    val GraphQL by lazy { load("/icons/symbol-graphql.svg") }
    val ApolloGraphQL by lazy { load("/icons/symbol-apollo-graphql.svg") }
  }

  object Action {
    val Apollo by lazy { load("/icons/action-apollo-monochrome.svg") }

    // This one cannot be lazy because it's referenced from plugin.xml
    @JvmField
    val ApolloColor = load("/icons/action-apollo-color.svg")
  }

  object ToolWindow {
    @JvmField
    val NormalizedCacheViewer = load("/icons/toolwindow-normalized-cache-viewer.svg")
  }

  object Node {
    val Package by lazy { load("/icons/node-package.svg") }
  }

  object StatusBar {
    val Apollo by lazy { load("/icons/status-apollo-monochrome.svg") }
  }

  private fun load(path: String): Icon {
    return IconLoader.getIcon(path, ApolloIcons::class.java)
  }
}
