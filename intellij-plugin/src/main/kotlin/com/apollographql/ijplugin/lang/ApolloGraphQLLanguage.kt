package com.apollographql.ijplugin.lang

import com.intellij.lang.Language

class ApolloGraphQLLanguage : Language("ApolloGraphQL") {
  companion object {
    @JvmField
    val INSTANCE: ApolloGraphQLLanguage = ApolloGraphQLLanguage()
  }
}
