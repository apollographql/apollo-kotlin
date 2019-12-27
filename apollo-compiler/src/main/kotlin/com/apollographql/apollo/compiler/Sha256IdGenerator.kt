package com.apollographql.apollo.compiler

class Sha256IdGenerator: CustomIdGenerator {
  override fun apply(queryString: String): String {
    return queryString.sha256()
  }

  override val version = "1.0"
}
