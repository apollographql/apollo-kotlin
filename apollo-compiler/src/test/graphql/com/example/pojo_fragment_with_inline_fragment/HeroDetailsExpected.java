package com.example.pojo_fragment_with_inline_fragment;

import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HeroDetails {
  public static final String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
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

  private final @Nonnull String name;

  private final @Nonnull FriendsConnection friendsConnection;

  private final @Nullable AsDroid asDroid;

  public HeroDetails(@Nonnull String name, @Nonnull FriendsConnection friendsConnection,
      @Nullable AsDroid asDroid) {
    this.name = name;
    this.friendsConnection = friendsConnection;
    this.asDroid = asDroid;
  }

  public @Nonnull String name() {
    return this.name;
  }

  public @Nonnull FriendsConnection friendsConnection() {
    return this.friendsConnection;
  }

  public @Nullable AsDroid asDroid() {
    return this.asDroid;
  }

  public static class FriendsConnection {
    private final @Nullable Integer totalCount;

    private final @Nullable List<? extends Edge> edges;

    public FriendsConnection(@Nullable Integer totalCount, @Nullable List<? extends Edge> edges) {
      this.totalCount = totalCount;
      this.edges = edges;
    }

    public @Nullable Integer totalCount() {
      return this.totalCount;
    }

    public @Nullable List<? extends Edge> edges() {
      return this.edges;
    }

    public static class Edge {
      private final @Nullable Node node;

      public Edge(@Nullable Node node) {
        this.node = node;
      }

      public @Nullable Node node() {
        return this.node;
      }

      public static class Node {
        private final @Nonnull String name;

        public Node(@Nonnull String name) {
          this.name = name;
        }

        public @Nonnull String name() {
          return this.name;
        }
      }
    }
  }

  public static class AsDroid {
    private final @Nonnull String name;

    private final @Nonnull FriendsConnection$ friendsConnection;

    private final @Nullable String primaryFunction;

    public AsDroid(@Nonnull String name, @Nonnull FriendsConnection$ friendsConnection,
        @Nullable String primaryFunction) {
      this.name = name;
      this.friendsConnection = friendsConnection;
      this.primaryFunction = primaryFunction;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public @Nonnull FriendsConnection$ friendsConnection() {
      return this.friendsConnection;
    }

    public @Nullable String primaryFunction() {
      return this.primaryFunction;
    }

    public static class FriendsConnection$ {
      private final @Nullable Integer totalCount;

      private final @Nullable List<? extends Edge> edges;

      public FriendsConnection$(@Nullable Integer totalCount,
          @Nullable List<? extends Edge> edges) {
        this.totalCount = totalCount;
        this.edges = edges;
      }

      public @Nullable Integer totalCount() {
        return this.totalCount;
      }

      public @Nullable List<? extends Edge> edges() {
        return this.edges;
      }

      public static class Edge {
        private final @Nullable Node node;

        public Edge(@Nullable Node node) {
          this.node = node;
        }

        public @Nullable Node node() {
          return this.node;
        }

        public static class Node {
          private final @Nonnull String name;

          public Node(@Nonnull String name) {
            this.name = name;
          }

          public @Nonnull String name() {
            return this.name;
          }
        }
      }
    }
  }
}
