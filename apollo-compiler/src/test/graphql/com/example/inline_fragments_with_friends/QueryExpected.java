package com.example.inline_fragments_with_friends;

import com.apollostack.api.GraphQLQuery;
import java.lang.Float;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class Query implements GraphQLQuery {
  public static final String OPERATION_DEFINITION = "query Query {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  ... on Human {\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "      }\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        id\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  @Override
  public String operationDefinition() {
    return OPERATION_DEFINITION;
  }

  @Override
  public List<String> fragmentDefinitions() {
    return Collections.emptyList();
  }

  public interface Data {
    @Nullable Hero hero();

    interface Hero {
      @Nonnull String name();

      @Nullable AsHuman asHuman();

      @Nullable AsDroid asDroid();

      interface AsHuman {
        @Nonnull String name();

        @Nullable List<Friend> friends();

        @Nullable Float height();

        interface Friend {
          @Nonnull String name();

          @Nonnull List<Episode> appearsIn();
        }
      }

      interface AsDroid {
        @Nonnull String name();

        @Nullable List<Friend> friends();

        @Nullable String primaryFunction();

        interface Friend {
          @Nonnull String name();

          long id();
        }
      }
    }
  }
}
