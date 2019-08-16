package com.apollographql.apollo.api;

/**
 * Represents a custom GraphQL scalar type
 */
public interface ScalarType {
  String typeName();

  Class javaType();
}
