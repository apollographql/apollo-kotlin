package com.example.scalar_types;

import com.apollographql.api.graphql.Operation;
import com.apollographql.api.graphql.Query;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  public interface Data extends Operation.Data {
    @Nullable String graphQlString();

    @Nullable String graphQlIdNullable();

    @Nonnull String graphQlIdNonNullable();

    @Nullable Integer graphQlIntNullable();

    int graphQlIntNonNullable();

    @Nullable Double graphQlFloatNullable();

    double graphQlFloatNonNullable();

    @Nullable Boolean graphQlBooleanNullable();

    boolean graphQlBooleanNonNullable();

    @Nullable List<? extends Integer> graphQlListOfInt();

    @Nullable List<? extends GraphQlListOfObject> graphQlListOfObjects();

    @Nullable List<? extends List<? extends Integer>> graphQlNestedList();

    interface GraphQlListOfObject {
      int someField();
    }
  }
}
