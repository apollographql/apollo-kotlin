package com.example.fragment_with_inline_fragment;

import com.apollostack.api.GraphQLQuery;
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
      + "    ...HeroDetails\n"
      + "    appearsIn\n"
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
        + "  ... on Droid {\n"
        + "    name\n"
        + "    primaryFunction\n"
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

      @Nonnull List<Episode> appearsIn();

      Fragments fragments();

      interface Fragments {
        HeroDetails heroDetails();
      }
    }
  }
}
