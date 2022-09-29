package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import kotlin.jvm.JvmStatic

@Deprecated("Use the version in apollo-ast instead or copy paste this implementation", ReplaceWith("com.apollographql.apollo3.ast.QueryDocumentMinifier"))
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_3_1)
object QueryDocumentMinifier {
  @JvmStatic
  fun minify(queryDocument: String): String {
    return queryDocument.replace("\\s *".toRegex(), " ")
  }
}
