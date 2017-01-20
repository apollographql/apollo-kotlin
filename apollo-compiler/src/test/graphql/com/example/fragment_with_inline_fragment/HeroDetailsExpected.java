package com.example.fragment_with_inline_fragment.fragment;

import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HeroDetails {
  String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
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
      + "}";

  String TYPE_CONDITION = "Character";

  @Nonnull String name();

  @Nonnull FriendsConnection friendsConnection();

  @Nullable AsDroid asDroid();

  interface FriendsConnection {
    @Nullable Integer totalCount();

    @Nullable List<? extends Edge> edges();

    interface Edge {
      @Nullable Node node();

      interface Node {
        @Nonnull String name();
      }
    }
  }

  interface AsDroid {
    @Nonnull String name();

    @Nonnull FriendsConnection$ friendsConnection();

    @Nullable String primaryFunction();

    interface FriendsConnection$ {
      @Nullable Integer totalCount();

      @Nullable List<? extends Edge> edges();

      interface Edge {
        @Nullable Node node();

        interface Node {
          @Nonnull String name();
        }
      }
    }
  }
}
