package com.apollographql.apollo.api.internal

object QueryDocumentMinifier {

  @JvmStatic
  fun minify(queryDocument: String): String = queryDocument.replace("\\s *".toRegex(), " ")
}
