package com.apollographql.apollo3.ast

import kotlin.jvm.JvmStatic

object QueryDocumentMinifier {
  @JvmStatic
  fun minify(queryDocument: String): String {
    return queryDocument.replace("\\s *".toRegex(), " ")
  }
}
