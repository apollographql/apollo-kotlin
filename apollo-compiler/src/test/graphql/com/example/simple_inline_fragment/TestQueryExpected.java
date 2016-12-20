package com.example.simple_inline_fragment;

import com.apollostack.api.GraphQLOperation;
import com.apollostack.api.GraphQLQuery;
import java.lang.Double;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements GraphQLQuery<GraphQLOperation.Variables> {
  public static final String OPERATION_DEFINITION = "query Query {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
      + "    }\n"
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
      @Nonnull String name();

      @Nullable AsHuman asHuman();

      @Nullable AsDroid asDroid();

      interface AsHuman {
        @Nonnull String name();

        @Nullable Double height();
      }

      interface AsDroid {
        @Nonnull String name();

        @Nullable String primaryFunction();
      }
    }
  }
}
