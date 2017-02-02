package com.example.fragment_friends_connection.fragment;

import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
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

        interface Factory {
          Creator creator();
        }

        interface Creator {
          Node create(@Nonnull String name);
        }
      }

      interface Factory {
        Creator creator();

        Node.Factory nodeFactory();
      }

      interface Creator {
        Edge create(@Nullable Node node);
      }
    }

    interface Factory {
      Creator creator();

      Edge.Factory edgeFactory();
    }

    interface Creator {
      FriendsConnection create(@Nullable Integer totalCount, @Nullable List<? extends Edge> edges);
    }
  }

  interface Factory {
    Creator creator();

    FriendsConnection.Factory friendsConnectionFactory();
  }

  interface Creator {
    HeroDetails create(@Nonnull String name, @Nonnull FriendsConnection friendsConnection);
  }
}
