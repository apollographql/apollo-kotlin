package com.apollographql.apollo.api

/**
 * Represents a custom GraphQL scalar type
 */
interface ScalarType {
  fun typeName(): String

  fun javaType(): Class<*>
}
