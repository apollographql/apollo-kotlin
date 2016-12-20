package com.example.directives;

import com.apollostack.api.GraphQLOperation;
import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nullable;

public final class TestQuery implements GraphQLQuery<GraphQLOperation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name @include(if: false)\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final GraphQLOperation.Variables variables;

  public TestQuery() {
    this.variables = GraphQLOperation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public GraphQLOperation.Variables variables() {
    return variables;
  }

  public interface Data extends GraphQLOperation.Data {
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}
