package com.example.fragment_friends_connection;

import com.apollostack.api.Query;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Query.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    ...HeroDetails\n"
      + "  }\n"
      + "}";

  public static final List<String> FRAGMENT_DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
    "fragment HeroDetails on Character {\n"
        + "  __typename\n"
        + "  name\n"
        + "  friendsConnection {\n"
        + "    totalCount\n"
        + "    edges {\n"
        + "      node {\n"
        + "        __typename\n"
        + "        name\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "}"
  ));

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
      Fragments fragments();

      interface Fragments {
        HeroDetails heroDetails();
      }
    }
  }
}
