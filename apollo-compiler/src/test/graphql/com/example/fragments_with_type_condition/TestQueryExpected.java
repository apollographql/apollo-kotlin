package com.example.fragments_with_type_condition;

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
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    ...HumanDetails\n"
      + "    ...DroidDetails\n"
      + "  }\n"
      + "  luke: hero {\n"
      + "    __typename\n"
      + "    ...HumanDetails\n"
      + "    ...DroidDetails\n"
      + "  }\n"
      + "}";

  public static final List<String> FRAGMENT_DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
    "fragment HumanDetails on Human {\n"
        + "  name\n"
        + "  height\n"
        + "}","fragment DroidDetails on Droid {\n"
        + "  name\n"
        + "  primaryFunction\n"
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
    @Nullable R2 r2();

    @Nullable Luke luke();

    interface R2 {
      Fragments fragments();

      interface Fragments {
        HumanDetails humanDetails();

        DroidDetails droidDetails();
      }
    }

    interface Luke {
      Fragments fragments();

      interface Fragments {
        HumanDetails humanDetails();

        DroidDetails droidDetails();
      }
    }
  }
}
