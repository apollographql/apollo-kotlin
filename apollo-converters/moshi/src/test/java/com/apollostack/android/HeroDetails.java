package com.apollostack.android;

import com.apollostack.api.GraphQLQuery;

import java.util.List;

import javax.annotation.Nullable;

public final class HeroDetails implements GraphQLQuery<GraphQLQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query HeroDetails {\n"
      + "  allPeople {\n"
      + "    people {\n"
      + "      name\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final GraphQLQuery.Variables variables;

  public HeroDetails() {
    this.variables = GraphQLQuery.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public GraphQLQuery.Variables variables() {
    return variables;
  }

  public interface Data extends GraphQLQuery.Data {
    @Nullable AllPeople allPeople();

    interface AllPeople {
      @Nullable List<? extends People> people();

      interface People {
        @Nullable String name();
      }
    }
  }
}