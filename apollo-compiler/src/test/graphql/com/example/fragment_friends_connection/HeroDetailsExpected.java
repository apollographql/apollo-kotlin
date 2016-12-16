package com.example.fragment_friends_connection;

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

  @Nonnull String name();

  @Nonnull FriendsConnection friendsConnection();

  interface FriendsConnection {
    @Nullable Integer totalCount();

    @Nullable List<Edge> edges();

    interface Edge {
      @Nullable Node node();

      interface Node {
        @Nonnull String name();
      }
    }
  }
}
