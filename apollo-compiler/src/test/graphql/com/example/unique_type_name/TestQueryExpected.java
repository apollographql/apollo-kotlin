package com.example.unique_type_name;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Query;
import com.example.unique_type_name.fragment.HeroDetails;
import com.example.unique_type_name.type.Episode;
import java.lang.Double;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    friends {\n"
      + "      __typename\n"
      + "      name\n"
      + "    }\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "        friends {\n"
      + "          __typename\n"
      + "          ...HeroDetails\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
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
      @Nonnull String name();

      @Nullable List<? extends Friend> friends();

      @Nullable AsHuman asHuman();

      interface Friend {
        @Nonnull String name();
      }

      interface AsHuman {
        @Nonnull String name();

        @Nullable List<? extends Friend$> friends();

        @Nullable Double height();

        interface Friend$ {
          @Nonnull String name();

          @Nonnull List<? extends Episode> appearsIn();

          @Nullable List<? extends Friend$$> friends();

          interface Friend$$ {
            Fragments fragments();

            interface Fragments {
              HeroDetails heroDetails();
            }
          }
        }
      }
    }
  }
}
