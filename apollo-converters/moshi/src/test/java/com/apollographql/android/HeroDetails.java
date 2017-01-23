package com.apollographql.android;

import com.apollographql.api.graphql.Query;

import java.util.List;

import javax.annotation.Nullable;

public final class HeroDetails implements Query<Query.Variables> {
  public static final String OPERATION_DEFINITION = "query HeroDetails {\n"
      + "  allPeople {\n"
      + "    people {\n"
      + "      name\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Query.Variables variables;

  public HeroDetails() {
    this.variables = Query.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Query.Variables variables() {
    return variables;
  }

  public interface Data extends Query.Data {
    @Nullable AllPeople allPeople();

    interface AllPeople {
      @Nullable List<? extends People> people();

      interface People {
        @Nullable String name();
      }
    }
  }
}