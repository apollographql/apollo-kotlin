package com.apollographql.apollo3.api

import kotlin.jvm.JvmStatic

object QueryDocumentMinifier {
  @JvmStatic
  fun minify(queryDocument: String): String {
    return queryDocument.replace("\\s *".toRegex(), " ")
  }
}
