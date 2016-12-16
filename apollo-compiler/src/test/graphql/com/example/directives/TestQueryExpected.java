package com.example.directives;

import com.apollostack.api.Query;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Query.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name @include(if: false)\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Query.Variables variables;

  public TestQuery() {
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
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}
