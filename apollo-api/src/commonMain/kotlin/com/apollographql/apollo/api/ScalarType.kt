package com.apollographql.apollo.api

/**
 * Represents a custom GraphQL scalar type
 */
interface ScalarType {

  /**
   * GraphQL schema custom scalar type name (e.g. `ID`, `URL`, `DateTime` etc.)
   */
  fun typeName(): String

  /**
   * Fully qualified class name this GraphQL scalar type is mapped to (e.g. `java.lang.String`, `java.net.URL`, `java.util.DateTime`)
   */
  fun className(): String
}
