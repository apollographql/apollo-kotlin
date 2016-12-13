package com.example.scalar_types;

import com.apollostack.api.Query;
import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class ScalarTypes implements Query {
  public static final String OPERATION_DEFINITION = "";

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return Collections.emptyList();
  }

  public interface Data extends Query.Data {
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
}
