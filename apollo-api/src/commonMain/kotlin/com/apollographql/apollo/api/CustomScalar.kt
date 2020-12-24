package com.apollographql.apollo.api

/**
 * Represents a mapping from a custom GraphQL scalar type to a Java/Kotlin class
 */
data class CustomScalar(
  /**
   * GraphQL schema custom scalar type name (e.g. `ID`, `URL`, `DateTime` etc.)
   */
  val graphqlName: String,

  /**
   * Fully qualified class name this GraphQL scalar type is mapped to (e.g. `java.lang.String`, `java.net.URL`, `java.util.DateTime`)
   */
  val className: String
) {
  // Do not remove, this is used by generated code
  companion object
}
