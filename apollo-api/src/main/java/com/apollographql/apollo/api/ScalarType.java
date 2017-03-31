package com.apollographql.apollo.api;

public interface ScalarType {
  String typeName();

  Class javaType();
}
