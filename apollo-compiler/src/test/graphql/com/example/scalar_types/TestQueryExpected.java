package com.example.scalar_types;

import com.apollostack.api.Query;
import java.lang.Boolean;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Query.Variables> {
  public static final String OPERATION_DEFINITION = "";

  public static final List<String> FRAGMENT_DEFINITIONS = Collections.unmodifiableList(Collections.<String>emptyList());

  private final String query;

  private final Query.Variables variables;

  public TestQuery() {
    this.variables = Query.EMPTY_VARIABLES;
    StringBuilder stringBuilder = new StringBuilder(OPERATION_DEFINITION);
    stringBuilder.append("\n");
    for (String fragmentDefinition : FRAGMENT_DEFINITIONS) {
      stringBuilder.append("\n");
      stringBuilder.append(fragmentDefinition);
    }
    query = stringBuilder.toString();
  }

  @Override
  public String operationDefinition() {
    return query;
  }

  @Override
  public Query.Variables variables() {
    return variables;
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
