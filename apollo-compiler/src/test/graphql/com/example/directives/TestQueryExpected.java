package com.example.directives;

import com.apollostack.api.Query;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Query.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name @include(if: false)\n"
      + "  }\n"
      + "}";

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
    @Nullable Hero hero();

    interface Hero {
      @Nullable String name();
    }
  }
}
