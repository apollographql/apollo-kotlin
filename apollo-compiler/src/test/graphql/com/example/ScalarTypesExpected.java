package com.example;

import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.List;

public interface ScalarTypes {
  String graphQlString();

  Long graphQlIdNullable();

  long graphQlIdNonNullable();

  Integer graphQlIntNullable();

  int graphQlIntNonNullable();

  Float graphQlFloatNullable();

  float graphQlFloatNonNullable();

  Boolean graphQlBooleanNullable();

  boolean graphQlBooleanNonNullable();

  List<Integer> graphQlListOfInt();

  List<SomeObject> graphQlListOfObjects();

  List<List<Integer>> graphQlNestedList();

  interface SomeObject {
    int someField();
  }
}
