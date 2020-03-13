package com.apollographql.apollo.api

/**
 * Represents a custom GraphQL scalar type
 */
actual interface ScalarType {
  actual fun typeName(): String

  fun javaType(): Class<*>
}
