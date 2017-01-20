package com.example.unique_type_name.fragment;

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
      + "}";

  String TYPE_CONDITION = "Character";

  @Nonnull String name();

  @Nonnull FriendsConnection friendsConnection();

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
}
