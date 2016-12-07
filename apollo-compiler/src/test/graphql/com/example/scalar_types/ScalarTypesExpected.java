package com.example.scalar_types;

import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public interface ScalarTypes {
  @Nullable String graphQlString();

  @Nullable Long graphQlIdNullable();

  long graphQlIdNonNullable();

  @Nullable Integer graphQlIntNullable();

  int graphQlIntNonNullable();

  @Nullable Float graphQlFloatNullable();

  float graphQlFloatNonNullable();

  @Nullable Boolean graphQlBooleanNullable();

  boolean graphQlBooleanNonNullable();

  @Nullable List<Integer> graphQlListOfInt();

  @Nullable List<GraphQlListOfObject> graphQlListOfObjects();

  @Nullable List<List<Integer>> graphQlNestedList();

  interface GraphQlListOfObject {
    int someField();
  }
}
