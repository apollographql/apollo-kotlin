package com.apollographql.ijplugin.psi

import com.apollographql.ijplugin.lang.ApolloGraphQLLanguage
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls

class ApolloGraphQLTokenType(debugName: @NonNls String) :
  IElementType(debugName, ApolloGraphQLLanguage.INSTANCE)
