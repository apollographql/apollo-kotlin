package com.apollographql.ijplugin.psi

import com.intellij.psi.tree.TokenSet

interface ApolloGraphQLExtendedElementTypes {
  companion object {
    val KEYWORDS: TokenSet = TokenSet.create(
        ApolloGraphQLElementTypes.QUERY_KEYWORD,
        ApolloGraphQLElementTypes.MUTATION_KEYWORD,
        ApolloGraphQLElementTypes.SUBSCRIPTION_KEYWORD,
        ApolloGraphQLElementTypes.FRAGMENT_KEYWORD,
        ApolloGraphQLElementTypes.ON_KEYWORD,
        ApolloGraphQLElementTypes.SCHEMA_KEYWORD,
        ApolloGraphQLElementTypes.TYPE_KEYWORD,
        ApolloGraphQLElementTypes.SCALAR_KEYWORD,
        ApolloGraphQLElementTypes.INTERFACE_KEYWORD,
        ApolloGraphQLElementTypes.IMPLEMENTS_KEYWORD,
        ApolloGraphQLElementTypes.ENUM_KEYWORD,
        ApolloGraphQLElementTypes.UNION_KEYWORD,
        ApolloGraphQLElementTypes.EXTEND_KEYWORD,
        ApolloGraphQLElementTypes.INPUT_KEYWORD,
        ApolloGraphQLElementTypes.DIRECTIVE_KEYWORD,
        ApolloGraphQLElementTypes.REPEATABLE_KEYWORD
    )

    val SINGLE_QUOTES: TokenSet = TokenSet.create(ApolloGraphQLElementTypes.OPEN_QUOTE, ApolloGraphQLElementTypes.CLOSING_QUOTE)
    val TRIPLE_QUOTES: TokenSet =
      TokenSet.create(ApolloGraphQLElementTypes.OPEN_TRIPLE_QUOTE, ApolloGraphQLElementTypes.CLOSING_TRIPLE_QUOTE)
    val QUOTES: TokenSet = TokenSet.orSet(SINGLE_QUOTES, TRIPLE_QUOTES)
    val STRING_TOKENS: TokenSet = TokenSet.orSet(QUOTES, TokenSet.create(ApolloGraphQLElementTypes.REGULAR_STRING_PART))
    val NUMBER_LITERALS: TokenSet = TokenSet.create(ApolloGraphQLElementTypes.NUMBER, ApolloGraphQLElementTypes.FLOAT)
  }
}
