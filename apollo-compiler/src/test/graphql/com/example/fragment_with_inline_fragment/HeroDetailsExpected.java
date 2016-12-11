package com.example.fragment_with_inline_fragment;

import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HeroDetails {
  @Nonnull String name();

  @Nonnull FriendsConnection friendsConnection();

  @Nullable AsDroid asDroid();

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

  interface AsDroid {
    @Nonnull String name();

    @Nonnull FriendsConnection$ friendsConnection();

    @Nullable String primaryFunction();

    interface FriendsConnection$ {
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
}
