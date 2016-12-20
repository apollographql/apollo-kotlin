package com.example.two_heroes_unique;

import com.apollostack.api.GraphQLOperation;
import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements GraphQLQuery<GraphQLOperation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    name\n"
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
    @Nullable R2 r2();

    @Nullable Luke luke();

    interface R2 {
      @Nonnull String name();
    }

    interface Luke {
      long id();

      @Nonnull String name();
    }
  }
}
