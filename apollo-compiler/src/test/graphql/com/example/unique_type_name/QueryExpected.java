package com.example.unique_type_name;

import com.apollostack.api.GraphQLQuery;
import java.lang.Float;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Query implements GraphQLQuery {
  public static final String OPERATION_DEFINITION = "query Query {\n"
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

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return FRAGMENT_DEFINITIONS;
  }

  public interface Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nullable List<Friend> friends();

      @Nullable AsHuman asHuman();

      interface Friend {
        @Nonnull String name();
      }

      interface AsHuman {
        @Nonnull String name();

        @Nullable List<Friend$> friends();

        @Nullable Float height();

        interface Friend$ {
          @Nonnull String name();

          @Nonnull List<Episode> appearsIn();

          @Nullable List<Friend$$> friends();

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
