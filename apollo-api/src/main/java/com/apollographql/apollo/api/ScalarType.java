package com.apollographql.apollo.api;

/**
 * Represents a custom graphQL scalar type
 */
public interface ScalarType {
  String typeName();

  Class javaType();
}
