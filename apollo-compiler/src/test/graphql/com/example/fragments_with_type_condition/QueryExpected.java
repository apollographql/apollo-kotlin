package com.example.fragments_with_type_condition;

import com.apollostack.api.GraphQLQuery;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public final class Query implements GraphQLQuery {
  public static final String OPERATION_DEFINITION = "query Query {\n"
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

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return FRAGMENT_DEFINITIONS;
  }

  public interface Data {
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
