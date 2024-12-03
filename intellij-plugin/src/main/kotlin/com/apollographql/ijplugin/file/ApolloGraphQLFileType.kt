package com.apollographql.ijplugin.file

import com.apollographql.ijplugin.icons.ApolloIcons
import com.apollographql.ijplugin.lang.ApolloGraphQLLanguage
import com.intellij.openapi.fileTypes.LanguageFileType

class ApolloGraphQLFileType : LanguageFileType(ApolloGraphQLLanguage.INSTANCE) {
  @Suppress("CompanionObjectInExtension")
  companion object {
    @JvmField
    val INSTANCE: ApolloGraphQLFileType = ApolloGraphQLFileType()

    val SUPPORTED_EXTENSIONS = setOf("graphqls", "graphql")
  }

  override fun getName() = "ApolloGraphQL"

  @Suppress("DialogTitleCapitalization")
  override fun getDescription() = "Apollo GraphQL"

  override fun getDefaultExtension() = "graphqls"

  override fun getIcon() = ApolloIcons.Symbol.ApolloGraphQL
}
