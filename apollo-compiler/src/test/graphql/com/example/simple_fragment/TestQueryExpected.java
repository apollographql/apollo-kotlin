package com.example.simple_fragment;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import com.example.simple_fragment.fragment.HeroDetails;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    ...HeroDetails\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

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
    @Nullable Hero hero();

    interface Hero {
      Fragments fragments();

      interface Fragments {
        HeroDetails heroDetails();
      }
    }
  }
}
